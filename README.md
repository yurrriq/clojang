clojang
=======

[![][clj-logo]][clj-logo]

[clj-logo]: resources/images/clj-logo.png

*A Clojure wrapper for Erlang's JInterface*

![Clojars Project](http://clojars.org/clojang/latest-version.svg)


##### Table of Contents

* [Introduction](#introduction-)
* [Usage](#dependencies-)
  * [Handle Exit](#handle-exit-)  
  * [Erlang-style Handler](#erlang-style-handler-)
* [Example](#usage-)
  * [A Simple Calculator in Clojure](#a-simple-calculator-in-clojure-)
  * [A Simple LFE Calculator API](#a-simple-lfe-calculator-api-)
  * [OTP Integration in LFE](#otp-integration-in-lfe-)
* [Erlang and JInterface](#erlang-and-jinterface-)
  * [A Note on Versions](#a-note-on-versions-)
  * [Setting Up Your Erlang's JInterface for Clojure](#setting-up-your-erlangs-jinterface-for-clojure-)
    * [Finding Your Root Dir](#finding-your-root-dir-)
    * [Finding Your JInterface Version](#finding-your-jinterface-version-)


## Introduction [&#x219F;](#table-of-contents)

This library is a rewrite of the
[clojure-erlastic](https://github.com/awetzel/clojure-erlastic)
library from which it was originally forked. It differs in how the code is
organized, code for wrapping and working with the Erlang types, code for
wrapping Erlang Java classes to more closely resemble Clojure idioms, and
the use of the
[Pulsar library](http://docs.paralleluniverse.co/pulsar/) by
[Parallel Universe](https://github.com/puniverse) to provide an
Erlang-like experience within Clojure (e.g., creating servers and
pattern matching on received messages).


## Usage [&#x219F;](#table-of-contents)

[to be updated]

`port-connection` creates two channels that you can use to
communicate respectively in and out with the calling erlang port.
The objects you put or receive throught these channels are encoded
and decoded into erlang binary term following these rules :

- erlang atom is clojure keyword
- erlang list is clojure list
- erlang tuple is clojure vector
- erlang binary is clojure bytes[]
- erlang integer is clojure long
- erlang float is clojure double
- erlang map is clojure map (thanks to erlang 17.0)
- clojure string is erlang binary (utf8)
- clojure set is erlang list

For instance, here is a simple echo server :

```clojure
(let [[in out] (clojure-erlastic.core/port-connection)]
  (<!! (go (while true
    (>! out (<! in))))))
```

### Handle Exit [&#x219F;](#table-of-contents)

[to be updated]

The channels are closed when the launching erlang application dies, so you just
have to test if `(<! in)` is `nil` to know if the connection with erlang is
still opened.  


### Erlang-style Handler [&#x219F;](#table-of-contents)

[to be updated]

In Java you cannot write a function as big as you want (the compiler may fail),
and the `go` and `match` macros expand into a lot of code. So it can be
useful to wrap your server with an "erlang-style" handler.

Clojure-erlastic provide the function `(run-server initfun handlefun)`
allowing you to easily develop a server using erlang-style handler :

- the `init` function must return the initial state
- the `handle` function must return `[:reply response newstate]`, or `[:noreply newstate]`

The argument of the init function is the first message sent by the erlang port
after starting.

```clojure
(require '[clojure.core.async :as async :refer [<! >! <!! go]])
(require '[clojure-erlastic.core :refer [run-server log]])
(use '[clojure.core.match :only (match)])

(run-server
  (fn [_] 0)
  (fn [term state] (match term
    [:add n] [:noreply (+ state n)]
    [:rem n] [:noreply (- state n)]
    :get [:reply state state])))

(log "end application, clean if necessary")
```

## Example [&#x219F;](#table-of-contents)

We'll tackle an example in two parts: an LFE part and a Clojure part. For both,
we'll need a shared project space:

```bash
$ lfetool new library calculator
$ mkdir -p calculator/src/clj
$ mkdir -p calculator/src/lfe
$ cd calculator
$ mv src/calculator* src/lfe/
$ export APPDIR=`pwd`
```

This code has also been added to the ``examples`` directory, so if you don't
have all the necesary tools installed, you are not excluded from the party :-)

### A Simple Calculator in Clojure [&#x219F;](#table-of-contents)

Now we can tackle the Clojure code that LFE will control. Let's create a project,
and then just take the bits we need.

```bash
$ cd src/clj
$ lein new app calculator
$ mv calculator/project.clj $APPDIR
$ mv calculator/src .
$ mv calculator/test .
$ rm -rf calculator
$ mv src/calculator . && rmdir src
```

Next, config changes:

```bash
$ vi $APPDIR/project.clj
```

```clojure
  ...
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clojang "0.3.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]]
  ...
  :source-paths ["src/clj"]
  :test-paths ["src/clj/test"]
  ...
```

and remove this line from your ``project.clj``:

```clojure
  :target-path "target/%s"
```

Now create your clojure server:

```bash
$ vi calculator/core.clj
```

```clojure
(ns calculator.core
  (:require [clojure.core.async :as async :refer [<! >! <!! chan go close!]]
            [clojang.core :as clojang]
            [clojure.core.match :refer [match]])
  (:gen-class))

(defn -main [& args]
  (let [[in out] (clojang/port-connection)]
    (<!! (go
      (loop [num 0]
        (match (<! in)
          [:add n] (recur (+ num n))
          [:rem n] (recur (- num n))
          :get (do (>! out num) (recur num))))))))
```

```bash
$ lein uberjar
```

### A Simple LFE Calculator API [&#x219F;](#table-of-contents)

Now we'll set up the LFE portion of the project:

```bash
$ cd $APPDIR/
$ vi rebar.config
```

Change the ``src`` directory to poing to the new ``src/lfe`` directory you
created:

```erlang
...
{erl_opts, [debug_info, {src_dirs, ["test", "src/lfe"]}]}.
...
```

```bash
$ vi src/lfe/calculator.lfe
```

Remove the stubbed function and replace it with the following code that will
talk to Clojure's port:

```lfe
(defmodule calculator
  (export all))

(defun java () "java -jar ")
(defun jar () "target/calculator-0.1.0-SNAPSHOT-standalone.jar ")
(defun args () " ")
(defun start-cmd () "lein run")
(defun port-name () 'lfecljport)

(defun start ()
  (register
    (port-name)
    (erlang:open_port ;;`#(spawn ,(++ (java) (jar) (args)))
                      `#(spawn ,(start-cmd))
                      '(binary use_stdio #(packet 4))))
  #(ok started))

(defun cmd (data)
  (! (whereis (port-name)) `#(,(self) #(command ,(term_to_binary data))))
  (receive
    (`#(data ,data) (binary_to_term data))
    (x (io:format x))))

(defun incr (int)
  (cmd `#(add ,int)))

(defun decr (int)
  (cmd `#(rem ,int)))
```

Now we can build everything an take it for a spin:

```bash
$ make calc
```

Once in the REPL:

```lfe
> (calculator:inc 2)
...
```

### OTP Integration in LFE [&#x219F;](#table-of-contents)

[to be updated -- the orginal example was in Elixir and the new one will be written in LFE, Erlang's Lisp]


Then create the OTP application and its root supervisor launching `Calculator`.

[TBD]

## Erlang and JInterface [&#x219F;](#table-of-contents)

You almost certainly do not want to use the JInterface dependencies that
are provided in Clojars. An explanation is given below with simple instructions
for making the JInterface for your Erlang version available to Clojure.


### A Note on Versions [&#x219F;](#table-of-contents)

JInterface is only guaranteed to work with the version of Erlang with which it
was released. The following version numbers are paired:

| Erlang Release | Erlang Version (erts) | JInterface |
|----------------|-----------------------|------------|
| 18.0           | 7.0                   | 1.6        |
| 17.5           | 6.4                   | 1.5.12     |
| 17.4           | 6.3                   | 1.5.11     |
| 17.3           | 6.2                   | 1.5.10     |
| 17.2           | 6.1                   | 1.5.9      |
| 17.1           | 6.1                   | 1.5.9      |
| 17.0           | 6.0                   | 1.5.9      |
| R16B03         | 5.10.4                | 1.5.8      |
| R16B02         | 5.10.3                | 1.5.8      |
| R16B01         | 5.10.2                | 1.5.8      |
| R16B           | 5.10.1                | 1.5.8      |
| R15B03         | 5.9.3                 | 1.5.6      |
| R15B02         | 5.9.2                 | 1.5.6      |
| R15B01         | 5.9.1                 | 1.5.6      |
| R15B           | 5.9                   | 1.5.5      |


### Setting Up Your Erlang's JInterface for Clojure [&#x219F;](#table-of-contents)

To ensure that your version of JInterface is ready for use by Clojure with your
version of Erlang, simply do this:

```bash
$ make jinterface
```

This will discover the Erlang root directory for the first ``erl`` found in your
``PATH``. It will also location the JInterface ``.jar`` file for that version
of Erlang.

If you wish to override these, you may do the following:

```
make jinterface ERL_LIBS=/opt/erlang/15.3.1
```

This ``make`` target (which depends upon Maven being installed) will
generate a ``lein``-friendly ``.jar`` file for you in your
``~/.m2/repository`` directory, just like ``lein`` does with downloaded Clojars.


#### Finding Your Root Dir [&#x219F;](#table-of-contents)

If you don't know what your Erlang root directory is, just fire up Erlang and
do the following:


```
$ erl
```
```erlang
1> code:root_dir().
"/opt/erlang/18.0"
```

The ``Makefile`` uses this to get the default Erlang root directory:

```
ERL_LIBS=$(erl -eval "io:format(code:root_dir()),halt()" -noshell)
```

#### Finding Your JInterface Version [&#x219F;](#table-of-contents)

With your ``ERL_LIBS`` dir in hand, you can easily discover the JInterface
version:

```bash
$ ls -1 $ERL_LIBS/lib/|grep jinterface|awk -F- '{print $2}'
1.6
```

The ``Makefile`` uses something similar to obtain the JInterface version number.

