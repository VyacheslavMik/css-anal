(ns css-anal.core
  (:require [clojure.java.io :refer [file]]
            [clojure.string :as str]

            [css-anal.css-props :refer [css-props]]
            [css-anal.html-tags :refer [tags]])
  (:gen-class))

(defn clojure-file? [file]
  (let [s (str file)
        d (str/last-index-of s ".")]
    (when d
      (#{#_".clj" ".cljs" #_".cljc"} (subs s d)))))

(defn clojure-files [dir]
  (let [d (file dir)]
    (filter clojure-file? (file-seq d))))

(defn read-one [r]
  (try
    (read r)
    (catch java.lang.RuntimeException e
      (if (= "EOF while reading" (.getMessage e))
        ::EOF
        (throw e)))))

(defn read-seq-from-file
  "Reads a sequence of top-level objects in file at path."
  [path]
  (let [s (slurp path)
        s (-> s
              (str/replace #"::\w+" "nil")
              (str/replace #"#js" ""))]
    (with-open [r (java.io.PushbackReader. (java.io.StringReader. s))]
      (binding [*read-eval* false]
        (doall (take-while #(not= ::EOF %) (repeatedly #(read-one r))))))))

(defn extract-hiccups [sexp]
  (when sexp
    (if (and (vector? sexp) (keyword? (first sexp)))
      sexp
      (->> sexp
           (remove (complement coll?))
           (mapcat extract-hiccups)
           vec))))

(defn html-tag? [k]
  (when-let [tag (and (keyword? k)
                      (some->> (name k)
                               (re-matches #"^(\w+)\.?\#?.*")
                               second
                               keyword))]
    (and (tags tag))))

(defn extract-classes [hc]
  (cond
    (vector? hc) (mapcat extract-classes hc)
    (html-tag? hc) (re-seq #"\.\w+-*_*\w+" (name hc))
    :else nil))

(defn css-class-name? [k]
  (and (keyword? k) (str/starts-with? (name k) ".")))

(defn css-pseudo-class? [k]
  (and (keyword? k) (str/starts-with? (name k) "&")))

(defn webkit-prop [k]
  (str/starts-with? (name k) "-webkit"))

(defn css-properties? [m]
  (and (map? m)
       (every? (fn [[k _]]
                 (and (or (keyword? k) (string? k))
                      (or (css-props (name k))
                          (webkit-prop k))))
               m)))

;; FIXME implement properly working function
(defn garden-fn? [sexp aliases]
  (list? sexp))

(defn html-tag? [k]
  (when (keyword? k)
    (let [k (keyword (first (str/split (name k) #"\.")))]
      (tags k))))

(defn selector? [k]
  (or (css-class-name? k)
      (css-pseudo-class? k)
      (garden-fn? k nil)
      (= k :*)
      (html-tag? k)))

(defn css-style? [sexp aliases]
  (and (coll? sexp)
       (selector? (first sexp))
       (or (css-properties? (second sexp))
           (every? (fn [sexp] (css-style? sexp aliases)) (rest sexp)))))

(defn css-styles [sexp aliases]
  (if (sequential? sexp)
    (loop [[h & t] sexp
           acc []]
      (if h
        (if (css-style? h aliases)
          (recur t (conj acc h))
          (recur t (vec (concat acc (css-styles h aliases)))))
        acc))
    []))

(defn nested-styles [style]
  (if (css-style? style nil)
    (let [[f & r :as style] (drop-while selector? style)]
      (if (css-properties? f) r style))
    []))

(defn html-tag-selector-class-names [selector]
  (mapv (fn [s] (keyword (str "." s))) (rest (str/split (name selector) #"\."))))

(defn class-names [style]
  (if (css-style? style nil)
    (let [selectors (take-while selector? style)]
      (reduce (fn [acc selector]
                (cond
                  (css-class-name? selector) (conj acc selector)
                  (html-tag? selector) (vec (concat acc (html-tag-selector-class-names selector)))
                  :else acc))
              [] selectors))
    []))

(defn css-class-names [style]
  (vec (concat (class-names style) (vec (mapcat css-class-names (nested-styles style))))))

(defn cljs-file-css-class-names [file]
  (mapcat css-class-names (css-styles (read-seq-from-file file) nil)))

(defn project-css-class-names [path]
  (mapcat cljs-file-css-class-names (clojure-files path)))

(defn get-all-classes [dir]
  (mapcat #(-> %
               read-seq-from-file
               extract-hiccups
               extract-classes
               distinct) (clojure-files dir)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(comment
  (get-all-classes "/home/evgeny/css-anal/src/css_anal")

  (def sample (read-seq-from-file (second (clojure-files "/home/evgeny/css-anal/src/css_anal_tmp/"))))

  (-> sample
      extract-hiccups
      extract-classes
      distinct)

  (def sample (read-seq-from-file (second (clojure-files "/Users/vyacheslavmikushev/Work/css-anal"))))

  (extract-hiccups sample)

  )
