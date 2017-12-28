(ns css-anal.core
  (:require [clojure.java.io :refer [file]]
            [clojure.string :as str]

            [css-anal.css-props :refer [css-props]])
  (:gen-class))

(defn clojure-file? [file]
  (let [s (str file)
        d (str/last-index-of s ".")]
    (when d
      (#{#_".clj" ".cljs" #_".cljc"} (subs s d)))))

(defn clojure-files [dir]
  (let [d (file dir)]
    (filter clojure-file? (file-seq d))))

(defn read-one
  [r]
  (try
    (read r)
    (catch java.lang.RuntimeException e
      (if (= "EOF while reading" (.getMessage e))
        ::EOF
        (throw e)))))

(defn read-seq-from-file
  "Reads a sequence of top-level objects in file at path."
  [path]
  (with-open [r (java.io.PushbackReader. (clojure.java.io/reader path))]
    (binding [*read-eval* false]
      (doall (take-while #(not= ::EOF %) (repeatedly #(read-one r)))))))

(defn extract-hiccups [sexp]
  (when sexp
    (if (and (vector? sexp) (keyword? (first sexp)))
      sexp
      (->> sexp
           (remove symbol?)
           (mapcat extract-hiccups)
           vec))))

(defn extract-classes [hc]
  (cond
    (vector? hc) (mapcat extract-classes hc)
    (keyword? hc) (re-seq #"\.\w+" (name hc))
    :else nil))

(defn css-class-name? [k]
  (and (keyword? k) (str/starts-with? (name k) ".")))

(defn css-properties [m]
  (every? (fn [[k _]]
            (cond
              (keyword? k) (css-props (name k))
              (string? k) (css-props k)
              :else false))
          m))

(defn css-class? [sexp]
  (and (css-class-name? (first sexp))
       ))

;; (defn extract-garden-css [sexp]
;;   (

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(comment

  (def sample (read-seq-from-file (second (clojure-files "/home/evgeny/css-anal/src/css_anal_tmp/"))))

  (-> sample
      extract-hiccups
      extract-classes
      distinct)

  (def sample (read-seq-from-file (second (clojure-files "/Users/vyacheslavmikushev/Work/css-anal"))))

  (extract-hiccups sample)

  )
