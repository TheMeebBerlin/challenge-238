    (ns fallout.core
      (:require [clojure.java.io :as io]
                [clojure.string :as str]
                [clojure.core.async :as async]))

    ;; First load this namespace, then put guesses on the "guesses-chan" channel:
    (comment
      (clojure.core.async/put! fallout.core/guesses-chan "SETTLERS"))


    (def ^:const NUM_GUESSES 4)
    (def ^:const WORD_LENGTH 8)
    (def ^:const DIFFICULTY "Difficulty equals the number of words" 10)

    (def words (clojure.string/split-lines (slurp (io/resource "enable1.txt"))))

    (defn words-of-length
      [words length]
      (filter #(= (count %) length) words))

    (defn chars-matching
      "Returns the number of characters matching at equal positions in two strings"
      [s1 s2]
      (reduce + (map #(if (apply = %&) 1 0) s1 s2)))

    ;; Put input on this channel. Applies an upper-case transducer.
    (def guesses-chan (async/chan 1 (map str/upper-case)))

    (defn process-guess
      "Processes a single guess, returns a map describing the state of the game"
      [correct-answer guess answer-count]
      {:correct-guess? (= guess correct-answer)
       :matching       (str (chars-matching correct-answer guess) "/" WORD_LENGTH)
       :game-over?     (= answer-count NUM_GUESSES)})

    (defn start-game
      "Starts the game by returning a channel describing the result of a guess"
      [length difficulty]
      (let [words (->> (words-of-length words length) shuffle (map str/upper-case) (take difficulty))
            correct-answer (first (shuffle words))
            answer-count-chan (async/chan)]
        (println "The word I'm looking for is one of:\n")
        (doseq [w words]
          (println w))
        (async/onto-chan answer-count-chan (iterate inc 1))
        (async/map (partial process-guess correct-answer) [guesses-chan answer-count-chan])))


    (let [loop-chan (start-game WORD_LENGTH DIFFICULTY)]
      (async/go-loop []
        (let [{:keys [correct-guess? matching game-over?]} (async/<! loop-chan)]
          (cond
            correct-guess? (println "Correct!")
            game-over? (println "Game over!")
            :else (do
                    (println (str "Number of positions matching: " matching))
                    (recur))))))


