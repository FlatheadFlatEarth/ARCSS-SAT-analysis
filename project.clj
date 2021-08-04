(defproject arcss-sat "0.1.0-SNAPSHOT"
  :description "A project for parsing temperature data."
  :url "http://fe101.us"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [incanter "1.5.5"]
                 [org.clojure/data.csv "1.0.0"]]
  :plugins [[org.clojars.benfb/lein-gorilla "0.7.0"]]
  :repl-options {:init-ns arcss-sat.data.sat})
