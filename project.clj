(defproject datahike-experiments "0.1.0-SNAPSHOT"
  :description "experiments with datahike"
  :url "http://github.com/kordano/datahike-experiments"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.datomic/datomic-free "0.9.5697"]
                 [io.replikativ/datahike "0.2.1-SNAPSHOT"]]
  :repl-options {:init-ns datahike-experiments.core})
