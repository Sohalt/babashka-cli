(ns babashka.cli.completion-test
  (:require [babashka.cli :as cli :refer [complete]]
            [clojure.java.io :as io]
            [clojure.test :refer :all]))

(def cmd-table
  [{:cmds ["foo"] :spec {:foo-opt {:coerece :string
                                   :alias :f}
                         :foo-opt2 {:coerece :string}
                         :foo-flag {:coerce :boolean
                                    :alias :l}}}
   {:cmds ["foo" "bar"] :spec {:bar-opt {:coerce :keyword}
                               :bar-flag {:coerce :boolean}}}
   {:cmds ["bar"]}
   {:cmds ["bar-baz"]}])

(deftest completion-test
  (testing "complete commands"
    (is (= #{"foo" "bar" "bar-baz"} (set (complete cmd-table [""]))))
    (is (= #{"bar" "bar-baz"} (set (complete cmd-table ["ba"]))))
    (is (= #{"bar-baz"} (set (complete cmd-table ["bar"]))))
    (is (= #{"foo"} (set (complete cmd-table ["f"])))))

  (testing "no completions for full command"
    (is (= #{} (set (complete cmd-table ["foo"])))))

  (testing "complete subcommands and options"
    (is (= #{"bar" "-f" "--foo-opt" "--foo-opt2" "-l" "--foo-flag"} (set (complete cmd-table ["foo" ""])))))

  (testing "complete suboption"
    (is (= #{"-f" "--foo-opt" "--foo-opt2" "-l" "--foo-flag"} (set (complete cmd-table ["foo" "-"])))))

  (testing "complete short-opt"
    (is (= #{} (set (complete cmd-table ["foo" "-f"]))))
    (is (= #{} (set (complete cmd-table ["foo" "-f" ""]))))
    (is (= #{} (set (complete cmd-table ["foo" "-f" "foo-val"]))))
    (is (= #{} (set (complete cmd-table ["foo" "-f" "bar"]))))
    (is (= #{} (set (complete cmd-table ["foo" "-f" "foo-flag"]))))
    (is (= #{} (set (complete cmd-table ["foo" "-f" "foo-opt2"]))))
    (is (= #{} (set (complete cmd-table ["foo" "-f" "123"]))))
    (is (= #{} (set (complete cmd-table ["foo" "-f" ":foo"]))))
    (is (= #{} (set (complete cmd-table ["foo" "-f" "true"]))))
    (is (= #{"bar" "--foo-opt2" "-l" "--foo-flag"} (set (complete cmd-table ["foo" "-f" "foo-val" ""])))))

  (testing "complete option with same prefix"
    (is (= #{"--foo-opt" "--foo-opt2" "--foo-flag"} (set (complete cmd-table ["foo" "--foo"]))))
    (is (= #{"--foo-opt2"} (set (complete cmd-table ["foo" "--foo-opt"])))))

  (testing "complete long-opt"
    (is (= #{} (set (complete cmd-table ["foo" "--foo-opt2"]))))
    (is (= #{} (set (complete cmd-table ["foo" "--foo-opt" ""]))))
    (is (= #{} (set (complete cmd-table ["foo" "--foo-opt" "foo-val"]))))
    (is (= #{} (set (complete cmd-table ["foo" "--foo-opt" "bar"]))))
    (is (= #{} (set (complete cmd-table ["foo" "--foo-opt" "foo-flag"]))))
    (is (= #{} (set (complete cmd-table ["foo" "--foo-opt" "foo-opt2"]))))
    (is (= #{} (set (complete cmd-table ["foo" "--foo-opt" "123"]))))
    (is (= #{} (set (complete cmd-table ["foo" "--foo-opt" ":foo"]))))
    (is (= #{} (set (complete cmd-table ["foo" "--foo-opt" "true"]))))
    (is (= #{"bar" "--foo-opt2" "-l" "--foo-flag"} (set (complete cmd-table ["foo" "--foo-opt" "foo-val" ""])))))

  (is (= #{"--foo-flag"} (set (complete cmd-table ["foo" "--foo-f"]))))

  (testing "complete short flag"
    (is (= #{} (set (complete cmd-table ["foo" "-l"]))))
    (is (= #{"bar" "-f" "--foo-opt" "--foo-opt2"} (set (complete cmd-table ["foo" "-l" ""])))))

  (testing "complete long flag"
    (is (= #{} (set (complete cmd-table ["foo" "--foo-flag"]))))
    (is (= #{"bar" "-f" "--foo-opt" "--foo-opt2"} (set (complete cmd-table ["foo" "--foo-flag" ""])))))

  (is (= #{"-f" "--foo-opt" "--foo-opt2"} (set (complete cmd-table ["foo" "--foo-flag" "-"]))))
  (is (= #{"bar"} (set (complete cmd-table ["foo" "--foo-flag" "b"]))))

  (testing "complete subcommand"
    (is (= #{"--bar-opt" "--bar-flag"} (set (complete cmd-table ["foo" "--foo-flag" "bar" ""]))))
    (is (= #{"--bar-opt" "--bar-flag"} (set (complete cmd-table ["foo" "--foo-flag" "bar" "-"]))))
    (is (= #{"--bar-opt" "--bar-flag"} (set (complete cmd-table ["foo" "--foo-flag" "bar" "--"]))))
    (is (= #{"--bar-opt" "--bar-flag"} (set (complete cmd-table ["foo" "--foo-flag" "bar" "--bar-"]))))
    (is (= #{"--bar-opt"} (set (complete cmd-table ["foo" "--foo-flag" "bar" "--bar-o"]))))
    (is (= #{} (set (complete cmd-table ["foo" "--foo-flag" "bar" "--bar-opt" "a"]))))
    (is (= #{"--bar-flag"} (set (complete cmd-table ["foo" "--foo-flag" "bar" "--bar-opt" "bar-val" ""]))))))


(deftest parse-opts-completion-test
  (cli/parse-opts ["--babashka.cli/completion-snippet" "zsh"] {:complete true})
  (cli/parse-opts ["--babashka.cli/complete" "zsh" "foo"] {:complete true}))

(deftest dispatch-completion-test
  (is (= (slurp (io/resource "resources/completion/completion.zsh")) (with-out-str (cli/dispatch cmd-table ["--babashka.cli/completion-snippet" "zsh"] {:completion {:command "myprogram"}}))))
  (is (= (slurp (io/resource "resources/completion/completion.bash")) (with-out-str (cli/dispatch cmd-table ["--babashka.cli/completion-snippet" "bash"] {:completion {:command "myprogram"}}))))
  (is (= (slurp (io/resource "resources/completion/completion.fish")) (with-out-str (cli/dispatch cmd-table ["--babashka.cli/completion-snippet" "fish"] {:completion {:command "myprogram"}}))))

  (is (= "compadd -- foo\n" (with-out-str (cli/dispatch cmd-table ["--babashka.cli/complete" "zsh" "myprogram f"] {:completion {:command "myprogram"}}))))
  (is (= "COMPREPLY+=( \"foo\" )\n" (with-out-str (cli/dispatch cmd-table ["--babashka.cli/complete" "bash" "myprogram f "] {:completion {:command "myprogram"}}))))
  (is (= "foo\n" (with-out-str (cli/dispatch cmd-table ["--babashka.cli/complete" "fish" "myprogram f "] {:completion {:command "myprogram"}})))))
