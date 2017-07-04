(ns why.visualization
  "Exports functions to use with Rhizome to visualize decision trees.

  Examples

  (require '[why.core :as w])
  (require '[why.visualization :as wv])
  (require '[rhizome.viz :as r])

  (let [decision
        (w/not
         (w/and (-> false (w/namely \"foo\"))
                (-> true (w/namely \"bar\"))))]
    (r/view-tree wv/branch?
                 wv/children
                 decision
                 :node->descriptor wv/node-descriptor
                 :edge->descriptor wv/edge-descriptor))"
  (:import [why.core Because Namely And Or Not If]))

(defprotocol GraphNode
  (children [this])
  (branch? [this])
  (node-descriptor [this])
  (edge-descriptor [a b]))

(defn- leaf [kind label]
  (with-meta
    (reify GraphNode
      (children [_] _)
      (branch? [_] false)
      (node-descriptor [_] {:label label})
      (edge-descriptor [_ _]))
    {:kind kind}))

(extend-protocol GraphNode
  Namely
  (children [_]
    [(leaf :yes "YES")
     (leaf :no "NO")])
  (branch? [_] true)
  (edge-descriptor [_ b]
    {:label (if (-> b meta :kind (= :yes)) "yes" "no")})
  (node-descriptor [{:keys [name]}]
    {:label name})

  And
  (branch? [_] true)
  (children [_]
    [(leaf :yes "YES")
     (leaf :no "NO")])
  (edge-descriptor [_ b]
    {:label (if (-> b meta :kind (= :yes)) "yes" "no")})
  (node-descriptor [{:keys [clauses]}]
    {:label (apply str (interpose " AND " (map (comp :label node-descriptor) clauses)))})

  Or
  (branch? [_] true)
  (children [_]
    [(leaf :yes "YES")
     (leaf :no "NO")])
  (edge-descriptor [_ b]
    {:label (if (-> b meta :kind (= :yes)) "yes" "no")})
  (node-descriptor [{:keys [clauses]}]
    {:label (apply str (interpose " OR " (map (comp :label node-descriptor) clauses)))})

  If
  (branch? [_] true)
  (children [{:keys [body else]}]
    [body else])
  (edge-descriptor [{:keys [body]} target]
    {:label (if (= target body) "yes" "no")})
  (node-descriptor [{:keys [condition]}]
    (node-descriptor condition))

  Not
  (branch? [_] true)
  (children [_]
    [(leaf :yes "NO")
     (leaf :no "YES")])
  (edge-descriptor [a b]
    {:label (if (-> b meta :kind (= :yes)) "yes" "no")})
  (node-descriptor [{:keys [clause]}]
    (node-descriptor clause))

  Because
  (branch? [_] false)
  (children [_] [])
  (edge-descriptor [_ _])
  (node-descriptor [{:keys [decision]}]
    {:label (if decision "YES" "NO")}))
