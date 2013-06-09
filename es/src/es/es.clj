(ns es.es
  (:gen-class)
  (:require 
   [clojure.pprint]
   [clojurewerkz.elastisch.rest :as esr ]
   [clojurewerkz.elastisch.rest.index :as esi]
   [clojurewerkz.elastisch.rest.document :as esd]))

(esr/connect! "http://192.168.100.81:9200")

(defn create-custom-index [name]
  "Create the index, all settings here are the same as the regular json. things like :store becomes \"store\", so its
   just more convenient "
  (let [mapping-types
          {:test 
            {        
             :_source {:enabled "false" }
             :_all    {:enabled "false" }
             :properties {:gram    {:type "string" :store "yes" :compress "true" :index_options "docs" :omit_norms "true"}
                          :freq    {:type "long"   :store "yes" :compress "true" :index_options "docs" :omit_norms "true" :index "not_analyzed"}
                          }}}]

    (esi/create name :settings
                {"number_of_shards" 1
                 "number_of_replicas" 0}
                :mappings mapping-types)))


(defn upload-file [file]
  "a quick and (very) dirty implementation, waiting between each batch so we wont hit a connection error.."
  (let [parts (partition-all 5000 (clojure.string/split-lines (slurp file)))]
    (doseq [lines parts]
      (dorun (pmap (fn [line]
                     (let [[gram freq] (clojure.string/split line #"\t")]
                       (esd/create "test" "test" {:gram gram :freq freq}))) lines))
      (Thread/sleep 10000))))


(defn -main []
  (create-custom-index "test")
  (upload-file "shlomi.txt")
  (esi/optimize "test" :refresh true :max_num_segments 1 :wait_for_merge true))
