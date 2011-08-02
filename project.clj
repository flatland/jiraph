(defproject jiraph "0.7.0-alpha6"
  :description "embedded graph db library for clojure"
  :dependencies [[clojure "1.2.0"]
                 [useful "0.5.4"]
                 [masai "0.5.2"]
                 [cereal "0.1.5"]
                 [retro "0.6.0-alpha1"]
                 [ego "0.1.1"]]
  :dev-dependencies [[protobuf "0.5.0-alpha1"]
                     [tokyocabinet "1.24.1-SNAPSHOT"]
                     [org.clojars.flatland/cake-marginalia "0.6.1"]]
  :tasks [protobuf.tasks cake-marginalia.tasks])
