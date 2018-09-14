# LODE Tooling

## What
[Live OWL Documentation Environment](http://www.github.com/essepuntato/LODE) (LODE), is a service that automatically extracts classes, object properties, data properties, named individuals, annotation properties, general axioms and namespace declarations from an OWL and OWL2 ontology, and renders them as ordered lists, together with their textual definitions, in a human-readable HTML page designed for browsing and navigation by means of embedded links.

We use LODE to render SWEET in a human readable manner. The visualizations are available from https://esipfed.github.io/stc/. Simply click on the SWEET documentation link.

The ***lode.py*** script essentially caches all the LODE responses and makes these available for improved navigation by the SWEET community.

## How to run lode.py
First clone the primary ESIP Semantic Technology Committee Website documentation as follows
```
$ git clone https://github.com/ESIPFed/stc.git && cd stc
$ cp lode.py . && python lode.py
```
N.B. lode.py takes a long time to complete. However, once the script has completed, you will see a directory named **sweet_lode**, this contains all of the LODE visualizations. Having these saves A LOT of time compared to relying upon the LODE Webservices for each call.

## Issues/Bugs/Help

https://github.com/ESIPFed/sweet-tools/issues