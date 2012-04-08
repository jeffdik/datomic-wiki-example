(ns datomic-wiki-example.wiki
  (:require [datomic-wiki-example.helpers :as dh])
  (:use [clojure.pprint :only [pprint]]
        [datomic.api :only [db q] :as d]))

;; wikipage helpers
(def wikipage-schema [
                      ;; wikipage
                      {:db/id #db/id[:db.part/db]
                       :db/ident :wikipage/name
                       :db/valueType :db.type/string
                       :db/cardinality :db.cardinality/one
                       :db/fulltext true
                       :db/doc "A Wiki Page Name"
                       :db.install/_attribute :db.part/db}

                      {:db/id #db/id[:db.part/db]
                       :db/ident :wikipage/content
                       :db/valueType :db.type/string
                       :db/cardinality :db.cardinality/one
                       :db/fulltext true
                       :db/doc "A Wiki Page's Content"
                       :db.install/_attribute :db.part/db}
                      ])

(defn wikipage-by-name
  "Get a wikipage map by name"
  [db name]
  (dh/entity-by-attribute db :wikipage/name name))

(defn txInstants-for-wikipage-with-name
  "Transaction instants for a wikipage (newest first)"
  [db name]
  (dh/txInstants-for-entity-by-attribute db :wikipage/name name))

;; repl code

;; You connect to Datomic database with a URI
(def uri "datomic:mem://wiki")

;; Create the database
(d/create-database uri)

;; Create a database connection
(def conn (d/connect uri))

;; Commit the schema for the wikipage
(d/transact conn wikipage-schema)

;; Commit a "Foo" wikipage
(d/transact conn [{:db/id #db/id[:db.part/user]
                   :wikipage/name "Foo"
                   :wikipage/content "foo foo foo"}])

;; Commit a "Bar" wikipage
(d/transact conn [{:db/id #db/id[:db.part/user]
                   :wikipage/name "Bar"
                   :wikipage/content "bar bar bar"}])

;; Query the database for a wikipage named "Foo"
(def results (q dh/entity-id-by-attribute-query
                (db conn) :wikipage/name "Foo"))


;; See the results
results

;; Get ID for "Foo" wikipage
(def foo-id (ffirst results))
foo-id

;; Get a map of the "Foo" wikipage
(def foo (wikipage-by-name (db conn) "Foo"))
foo

;; Vandalism!!!
;; Corrupting an unadulterated triplet of "foo"es
(d/transact conn [{:db/id (:db/id foo)
                   :wikipage/name "Foo"
                   :wikipage/content "foo bar foo"}])

;; Get the new map of the "Foo" wikipage
(def foo (wikipage-by-name (db conn) "Foo"))
foo

;; Get a list (newest first) of all transaction "instants" that
;; manipulate Foo's data
(def foo-txs (txInstants-for-wikipage-with-name (db conn) "Foo"))
foo-txs

;; Display the map of the "Foo" wikipage at each point in time.
;; The key to this is the d/as-of function.
(pprint (map #(wikipage-by-name (d/as-of (db conn) %) "Foo")
             foo-txs))
