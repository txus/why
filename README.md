# why

Forget simplistic boolean arithmetic; embrace traceable decision trees.

[![Travis Badge](https://img.shields.io/travis/txus/why/master.svg)](https://travis-ci.org/txus/why "Travis Badge")

[![Clojars Project](http://clojars.org/txus/why/latest-version.svg)](http://clojars.org/txus/why)

```
The awful thing about life is this: everyone has their reasons.

-- Jean Renoir
```

## Rationale

Often we model business decisions in our code using boolean arithmetic
primitives such as `and`, `or` and `not`. Using these we construct opaque
predicates that evaluate to a final boolean. However the inner workings of these
predicates get lost and decisions become untraceable as soon as they're run.

Consider this hypothetical function that decides whether a user is authorized to
do something:

```clojure
(defn authorized? [user]
  (and (logged-in? user)
       (or (has-permission? user)
           (super-user? user))))
```

There are two different worlds in which this calling this function will evaluate
to `true`:

* The user is logged in and they have permission
* The user is logged in, doesn't have permission but is a super-user

Conversely, there are two worlds in which it will evaluate to `false`:

* The user is not logged in
* The user is logged in, doesn't have permission and is not a super-user

When this function evaluates to `true` or `false`, we might want to ask: _why?_ Exactly in which of these worlds am I? Enter, ehem, `why`.

## Usage

Let's see how we would model the previous decision as a decision tree in `why`:

```clojure
(require '[why.core :as w])

(defn authorized? [user]
  (w/and (-> (logged-in? user) (w/namely "is logged in"))
         (w/or (-> (has-permission? user) (w/namely "has permission"))
               (-> (super-user? user) (w/namely "is a super-user")))))
```

By replacing clojure's `and` and `or` with `why.core/and` and `why.core/or`
respectively, and by describing our booleans with `why.core/namely`, we built a
traceable decision tree. Let's try running it with `why.core/decide`:

```clojure
(w/decide (authorized? not-logged-in-user))
;; #why.core.Because{:decision false, :reasons [[:not "is logged in"]]}

(w/decide (authorized? logged-in-superuser))
;; #why.core.Because{:decision true, :reasons ["is logged in" "is a super-user"]}
```

These are no longer opaque boolean answers, but answers together with traces of
the decision paths.

### A more complex example

Here's a more complex example to show the remaining `why` primitives. Let's try
to model the case where a super-user is authorized always on staging
environments, and a normal user is authorized whenever it is not blacklisted
basket and it contains the right permissions:

```clojure

(defn complex-authorized? [user]
  (w/if* (-> (super-user? user) (w/namely "is super user"))
    (-> (staging-environment?) (w/namely "is staging"))
    (w/when (-> (w/not (blacklisted? user)) (w/namely "is not blacklisted"))
      (w/and (-> user :permissions (get "READ") (w/namely "can read"))
             (-> user :permissions (get "WRITE") (w/namely "can write"))))))
```

## Visualizing decision trees

Because decision trees are data, they're trivial to represent as a graph. The `why.visualization` namespace compiles decision trees to ![Rhizome](https://github.com/ztellman/rhizome) graphs (GraphViz).

```clojure
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
                :edge->descriptor wv/edge-descriptor))
```

![Graph](https://raw.githubusercontent.com/txus/why/master/resources/graph.png)

## Caveats

`if*` and `when` will evaluate their bodies at **construction time**, unlike
their lazy Clojure counterparts. That means anything in their bodies that
depends on their conditions being evaluated might break. For example, this will
break:

```clojure
;; will throw a NullPointerException if a is nil
(w/when (-> a (w/namely "a is there"))
  (odd? (* a 3)))
```

## License

The MIT License (MIT)

Copyright © 2017 Txus

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
