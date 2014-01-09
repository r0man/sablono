# ŜABLONO [![Build Status](https://travis-ci.org/r0man/sablono.png)](https://travis-ci.org/r0man/sablono)

Lisp/Hiccup style templating for Facebook's React in ClojureScript.

![](http://imgs.xkcd.com/comics/tags.png)

## Installation

Via Clojars: https://clojars.org/sablono

[![Current Version](https://clojars.org/sablono/latest-version.svg)](https://clojars.org/sablono)

## Usage

Most functions from [Hiccup](https://github.com/weavejester/hiccup)
are provided in the `sablono.core` namespace. The library can be used
with [Om](https://github.com/swannodette/om) like this:

	(ns example
	  (:require [om.core :as om :include-macros true]
				[sablono.core :as html :refer [html] :include-macros true]))

	(defn widget [data]
	  (om/component
	   (html [:div "Hello world!"
			  [:ul (for [n (range 1 10)]
					 [:li n])]
			  (html/submit-button "React!")])))

	(om/root {} widget js/document.body)

## Thanks

This library is based on James Reeves [Hiccup](https://github.com/weavejester/hiccup) library.

## License

Copyright © 2013 Roman Scherer

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
