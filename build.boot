(def project 'why)
(def version "0.1.0")

(set-env! :resource-paths #{"resources" "src"}
          :source-paths   #{"test"}
          :dependencies   '[[org.clojure/clojure "RELEASE"]
                            [adzerk/boot-test "RELEASE" :scope "test"]])

(task-options!
 pom {:project     project
      :version     version
      :description "Traceable business logic with decision trees"
      :url         "https://blog.txus.io/why"
      :scm         {:url "https://github.com/txus/why"}
      :license     {"The MIT License (MIT)"
                    "http://opensource.org/licenses/mit-license.php"}})

(deftask build
  "Build and install the project locally."
  []
  (comp (pom) (jar) (install)))

(deftask release
  []
  (comp (pom) (jar) (push)))

(require '[adzerk.boot-test :refer [test]])
