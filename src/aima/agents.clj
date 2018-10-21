(ns aima.agents
  (:require [clojure.pprint :refer [pprint]]))

(defn new-object
  "A `thing`: can represent any inanimate object.
  To personalize it just `assoc` the resulting map"
  [name]
  {:alive    false
   :name     name
   :location nil
   :type     :things})

(defn new-agent
  "Creates a new agent"
  [program name]
  {:alive       true
   :name        name
   :bump        false
   :holding     []
   :performance 0
   :program     program
   :location    nil
   :type        :agents})

(defn new-watcher
  "Attach a watcher to an environment.
  Good for debugging & teaching purposes"
  [a k h]
  (add-watch a k
             (fn [k a o n]
               (pprint "=== CHANGE ===")
               (pprint k)
               (pprint "OLD")
               (pprint o)
               (newline)
               (pprint "NEW")
               (pprint n)
               (newline)
               (swap! h update k conj o))))

(defn alive?
  "Is this thing alive?"
  [obj]
  (:alive obj))

(defn new-environment
  "Create a new environment to play with. `:name` is required,
  `:done?` can be added later and it holds a function to determine
  whether there's anything else to do in the environment, `:perceive`
  holds the function to let agents perceive their environment,
  `:execute` holds the function to let agents decide what to do
  considering their percepts"
  [name & [done? perceive execute]]
  (atom {:name   name
         :things []
         :agents []
         :step 0
         :max-steps 1000
         :done? done?
         :perceive perceive
         :execute execute}))

(defn perceive-&-run
  "Generic function to extract the program from an agent and if it is
  still alive run the program over percepts"
  [env percept agent]
  (let [f (:program agent)]
    (when (alive? agent)
      (map f (percept env agent)))))

(defn step
  [env]
  (let [done     (:done? @env)
        perceive (:perceive @env)
        execute  (:execute @env)
        agents   (:agents @env)]
    (when-not (done env)
      (let [actions (mapcat #(perceive-&-run env perceive %) agents)]
        (if (seq actions)
          (mapcat #(execute env %1 %2) agents actions)
          (mapcat #(execute env % nil) agents))))
    (swap! env update :step inc)))

(defn same-location?
  "Check whether a thing is at `location`"
  [obj location]
  (= (:location obj)
     location))

(defn same-name?
  [obj name-compare]
  (= (:name obj)
     name-compare))

(defn list-things
  ([env location]
   (let [things (:things @env)]
     (filter #(same-location? % location) things)))
  ([env location kind]
   (let [things (list-things env location)]
     (filter (fn [thing]
               (= (:name thing) kind)) things))))

(defn get-agent
  ([env]
   (:agents @env))
  ([env name]
   (filter (fn [ag]
             (= (:name ag) name)) (:agents @env))))

(defn location-empty?
  [env location]
  (empty? (list-things env location)))

(defn set-location
  "Place `obj` (can be whatever) at `location`"
  [obj location]
  (assoc obj :location location))

(defn add-thing
  "Add `thing` to the proper `env` slot at `location`"
  [env thing location]
  (let [t (:type thing)]
    (swap! env update t conj
           (set-location thing location))))

(defn remove-thing
  "Remove a `thing` from the given `env`"
  [env thing]
  (let [t (:type thing)]
    (swap! env assoc t (filterv (complement #{thing}) (t @env)))))

(defn done?
  [env & preds]
  (some false?
        (for [p preds]
          (p env))))