# Fiets
_Fiets_ is basically just another feeds aggregator tool. It's quite opinionated
in terms of its feature set and in terms of implementation style. And even in
its name - it doesn't have anything to do with bicycles by the way... ;-)

If you are interested in details, read on.
If you just want to dive in, download the current binary and head over to 
Quickstart.

## Features
* Fetch any number of feeds on a regular base and store posts in a unified stream.
* Supports RSS and Atom format.
* Fetch your personal Twitter feed.
* Bookmark posts for later reading.
* Fever API emulation. (As far as needed to make it work with the Reeder app.)
* Extend _fiets_ with custom Java classes:
    * Fetch anything you want (well, where posts can be extracted) from the Internet.
    * Implement filters to drop unwanted posts.
    * Implement views to highlight certain posts.

## Quickstart
(As _fiets_ is Java based, you need a working JRE on your path. Any 8+ 
version should do it.)

If you have an OPML file to import, you should do that first (if not, just
skip this step):

    java -cp fiets-0.9.jar fiets.opml.ImportOpml <filename.opml>

Next, start _fiets_:

    java -jar fiets-0.9.jar

By default it starts to listen at port 7000. You can choose an alternative port
as optional command line parameter.

Fiets _provides no means to secure the connection. When deploying it on a_
_public site make sure to run it behind a reverse proxy with authentication_
_and HTTPS!_

Database files are created in the current directory, log files below `logs`. 

_Fiets_ immediately starts checking known feeds for updates. 

### Add feeds
The add-feed bookmarklet on the Feeds page is currently broken and unfortunately
there is no form to add feeds, yet.

You can add feeds manually by calling the URL:

    http://<host>:<port>/add-feed/?url=<feed-url>

* `<feed-url>` has to point to a supported feed format.
* `<feed-url>` must be URL encoded. If you call add-feed from a browser, 
it should be just fine.

### From Source 
If you prefer to build from the current source:

```
git clone https://github.com/ondy/fiets.git
cd fiets
ant build
java -jar build/result/fiets-0.9.jar 
```
There are also some really barebone bash start and stop scripts for your convenience.

## Why?
I wrote _Fiets_ since my favorite aggregator is being discontinued. Of course
I could have adopted that tool, but I don't really like implementing
in PHP. 

Apart of that I have tried several feed aggregators over the years and
have always been missing certain features
so I took the chance now to implement my own one. And maybe it's also useful
for you, so here you are.
