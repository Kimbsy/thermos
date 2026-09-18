(defproject thermos "0.1.0-SNAPSHOT"
  :description "Messing about with thermal printers"
  :dependencies [[org.clojure/clojure "1.12.6"]
                 [com.fazecast/jSerialComm "2.11.4"]]
  :main ^:skip-aot thermos.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
