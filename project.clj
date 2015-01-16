(defproject cljs-alarmtube "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2657"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [om "0.8.0-rc1"]
                 [sablono "0.2.22"]]

  :plugins [[lein-cljsbuild "1.0.4"]]

  :source-paths ["src" "target/classes"]

  :clean-targets ["out/cljs_alarmtube" "cljs_alarmtube.js"]

  :cljsbuild {
    :builds [{:id "cljs-alarmtube"
              :source-paths ["src"]
              :compiler {
                :output-to "cljs_alarmtube.js"
                :output-dir "out"
                :optimizations :none
                :cache-analysis true
                :source-map true}}]})
