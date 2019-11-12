(ns datahike-experiments.migration
  (:require [datahike.db :as db]
            [datahike.api :as api]
            [datomic.api :as dt]
            [clojure.java.io :as io])
  (:import [datahike.datom Datom]))

(defn export-datoms
  "Export the database in a flat-file of datoms at path."
  [db path]
  (with-open [f (io/output-stream path)
              w (io/writer f)]
    (binding [*out* w]
      (doseq [d (db/-datoms db :eavt [])]
        (prn d)))))

(defn export-db
  "Export the database in a flat-file of entity-attribute-value-tx vector at path."
  [db path]
  (with-open [f (io/output-stream path)
              w (io/writer f)]
    (binding [*out* w]
      (doseq [^Datom d (db/-datoms db :eavt [])]
        (prn [(.-e d) (.-a d) (.-v d) (.-tx d)])))))


(defn import-datoms
  "Import a flat-file of datoms at path into your database."
  [conn path]
  (doseq  [datoms (->> (line-seq (io/reader path))
                       (map read-string)
                       (partition 1000))]
    (api/transact conn (vec datoms))))


(comment

  (def uri "datahike:mem:///migrate-me")

  (api/create-database uri)

  (def conn (api/connect uri))

  (api/transact conn [{:db/ident :name
                       :db/valueType :db.type/string
                       :db/cardinality :db.cardinality/one}])

  (api/transact conn [{:name "Alice"}
                      {:name "Bob"}
                      {:name "Charlie"}
                      {:name "Daisy"}])

  (export-db @conn "/tmp/dh_export")

  (def duri "datomic:mem://dtmc")

  (dt/create-database duri)

  (def dconn (dt/connect duri))

  @(dt/transact dconn [{:db/ident :name
                        :db/valueType :db.type/string
                        :db/cardinality :db.cardinality/one}])

  @(dt/transact dconn [{:name "Alice"}
                       {:name "Bob"}
                       {:name "Charlie"}
                       {:name "Daisy"}])

  @(dt/transact dconn [[:db/retractEntity 17592186045421]])

  (def schema-attrs #{:db/valueType :db/cardinality})

  (def schema (dt/pull (dt/db dconn) '[* {:db.install/valueType [*] :db.install/attribute [*] :db.install/function [*]}] :db.part/db))

  (def rschema  (->> (dt/q '[:find [?tx ...]
                             :in $ ?bf
                             :where
                             [?tx :db/txInstant ?inst]
                             [(<= ?inst ?bf)]]
                           (dt/db dconn) (java.util.Date. 70))
                     (mapcat (fn [db-tid]
                               (dt/q '[:find ?e ?at ?v
                                       :in $ ?t
                                       :where
                                       [?e ?a ?v _ ?added]
                                       [?a :db/ident ?at]]
                                     (dt/history (dt/db dconn))
                                     db-tid)))
                     (map (fn [[e a v :as ent]]
                            {e {:a a :v v}}))
                     (apply merge)))



  (def tx-history (let [txs (sort-by first (dt/q '[:find ?tx  ?inst
                                                   :in $ ?bf
                                                   :where
                                                   [?tx :db/txInstant ?inst]
                                                   [(< ?bf ?inst)]]
                                                 (dt/db dconn) (java.util.Date. 70)))]
                    (for [[tid :as tx] txs]
                      [tx (dt/q '[:find ?e ?at ?v ?added
                                  :in $ ?t
                                  :where
                                  [?e ?a ?v ?t ?added]
                                  [?a :db/ident ?at]]
                                (dt/history (dt/db dconn)) tid)])))

  (->> tx-history
       (mapcat (fn [[[tx _] tx-entities]]
              (mapv (fn [[e a v added]]
                      [e a (if (schema-attrs a)
                             (get-in rschema [v :v])
                             v) tx added]) tx-entities))))

  (->> tx-history
       (remove #(.after (java.util.Date. 70) (-> % first second))))

  (with-open [f (io/output-stream "/home/konrad/src/datahike.migration/resources/datomic.dat")
              w (io/writer f)]
    (binding [*out* w]
      (doseq [d (dt/datoms (dt/history (dt/db dconn)) :eavt)]
        (prn d)))))
