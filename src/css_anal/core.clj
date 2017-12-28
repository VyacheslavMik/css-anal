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
        s (str/replace s #"::\w+" "nil")]
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

(defn css-properties? [m]
  (and (map? m)
       (every? (fn [[k _]]
                 (cond
                   (keyword? k) (css-props (name k))
                   (string? k) (css-props k)
                   :else false))
               m)))

;; FIXME implement properly working function
(defn garden-fn? [sexp aliases]
  (list? sexp))

(defn html-tag? [k]
  (and (keyword? k) (tags k)))

(defn css-style? [sexp aliases]
  (and (coll? sexp)
       (or (css-class-name? (first sexp))
           (css-pseudo-class? (first sexp))
           (garden-fn? (first sexp) aliases)
           (html-tag? (first sexp)))
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

(defn conj-css-class-name [acc style]
  (if (css-class-name? (first style))
    (conj acc (first style))
    acc))

(defn css-class-names [style]
  (let [class-names (vec (mapcat css-class-names (if (css-properties? (second style))
                                                   (-> style rest rest)
                                                   (rest style))))]
    (if (css-class-name? (first style))
      (conj class-names (first style))
      class-names)))

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
