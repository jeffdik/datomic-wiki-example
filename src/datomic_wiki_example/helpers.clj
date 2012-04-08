(ns datomic-wiki-example.helpers
  (:use [datomic.api :only [db q] :as d]))

;; datomic helpers
(defn get-entity-map
  "Get a map of the entity's attributes"
  [db id]
  (let [entity (d/entity db id)]
    (reduce #(assoc %1 %2 (%2 entity))
           {:db/id id}
           (keys entity))))

(def entity-id-by-attribute-query
  "Datalog query to get an entity id by value"
  '[:find ?e
    :in $ ?attribute ?name
    :where
    [?e ?attribute ?name]])

(defn entity-by-attribute
  "Get an entity map by attribute"
  [db attribute value]
  (let [results (q entity-id-by-attribute-query db attribute value)]
    (case (count results)
      0 nil
      1 (get-entity-map db (ffirst results))
      :else (throw (Exception. (str (count results)
                                    " entities with "
                                    attribute " of " value))))))

(defn txInstants-for-entity-by-attribute
  "Transaction instants for an entity by attribute (newest first)"
  [db attribute value]
  (->> (q '[:find ?when
            :in $ ?attribute ?value
            :where
            [?wp ?attribute ?value]
            [?wp ?attr ?v ?tx]
            [?tx :db/txInstant ?when]]
          db
          attribute
          value)
       (apply concat)
       sort
       reverse))

