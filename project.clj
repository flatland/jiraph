(defproject jiraph "0.7.0-beta5"
  :description "embedded graph db library for clojure"
  :dependencies [[clojure "1.2.0"]
                 [masai "0.6.0-alpha1"]
                 [useful "0.7.0-beta5"]
                 [cereal "0.1.5"]
                 [retro "0.6.0-alpha2"]
                 [ego "0.1.5"]]
  :dev-dependencies [[protobuf "0.5.0-alpha4"]
                     ;; [org.clojars.flatland/cake-marginalia "0.6.3"]
                     [tokyocabinet "1.24.1-SNAPSHOT" :ext true]]
  ;; :tasks [protobuf.tasks cake-marginalia.tasks]
  :cake-plugins [[cake-protobuf "0.5.0-alpha5"]])
