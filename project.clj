(defproject clojang "0.3.0"
  :description "A Clojure wrapper forErlang's JInterface"
  :url "https://github.com/oubiwann/clojang"
  :scm {:name "git" :url "https://github.com/oubiwann/clojang"}
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.erlang.otp/jinterface "1.5.9"]
                 [trptcolin/versioneer "0.2.0"]]
  :repositories {"scalaris" "https://scalaris.googlecode.com/svn/maven/"})

