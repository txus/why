(ns why.core
  (:require [clojure.core :as clj])
  (:refer-clojure :exclude [and or not]))

(defprotocol Decidable
  "A Decidable can be turned into a Because"
  (decide [this]))

(defrecord Because [decision reasons]
  Decidable
  (decide [this] this))

(defn because
  "Constructs a reasoned decision. (because) constructs a decision of true with
  no reasons."
  ([] (because true))
  ([d] (because d []))
  ([d reasons]
   (->Because d reasons)))

(defn- because-+
  "Merges several reasoned decisions with an arbitrary binary operator. Their
  reasons will be selected depending on the final decision, if true, only the
  true reasons will be selected, otherwise only the false ones."
  [op-f becauses]
  (let [decision (reduce op-f (map :decision becauses))]
    (because decision (->> (if decision
                             (filter :decision becauses)
                             (remove :decision becauses))
                           (mapcat :reasons)
                           vec))))

(defrecord Namely [name predicate]
  Decidable
  (decide [_]
    (let [{:keys [decision] :as bc} (decide (predicate))]
      (update bc :reasons conj (if decision name [:not name])))))

(defn namely
  "Decorates a predicate or a value with a descriptive name."
  [predicate-or-value name]
  (->Namely name
            (if (fn? predicate-or-value)
              predicate-or-value
              (constantly predicate-or-value))))

(defrecord And [clauses]
  Decidable
  (decide [_]
    (because-+ #(clj/and %1 %2) (mapv decide clauses))))

(defrecord Or [clauses]
  Decidable
  (decide [_]
    (because-+ #(clj/or %1 %2) (mapv decide clauses))))

(defrecord If [condition body else]
  Decidable
  (decide [_]
    (let [{:keys [decision reasons]} (decide condition)]
      (-> (decide (if decision body else))
          (update :reasons into reasons)))))

(defrecord Not [clause]
  Decidable
  (decide [_]
    (-> (decide clause)
        (update :decision clj/not))))

(defn and
  "Constructs a conjunction of clauses (functions, named predicates, or plain
  values.)"
  [& clauses]
  (->And (vec clauses)))

(defn or
  "Constructs a disjunction of clauses (functions, named predicates, or plain
  values.)"
  [& clauses]
  (->Or (vec clauses)))

(defn not
  "Constructs a negation of a clause (functions, named predicates, or plain
  values.)"
  [clause]
  (->Not clause))

(defn if*
  "Branches a decision into two clauses: body and else."
  [condition body & [else]]
  (->If condition body (or else (because false))))

(defn when
  [condition body]
  (->If condition body (because false)))

(extend-protocol Decidable
  clojure.lang.Fn
  (decide [this] (because (this)))

  java.lang.Boolean
  (decide [this] (because this))

  nil
  (decide [_] (because nil))

  Object
  (decide [this]
    (because this)))

(defn decide-states
  "Given a map of states and decidables, it will decide each state and attach
  the reasons that make it true or false."
  [states]
  (->> states
       (map
        (fn [[state decidable]]
          [state (decide decidable)]))
       (into {})))
