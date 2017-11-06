Code to check for updates in the ontologies at the Github repo
and perform automatic registration of new versions.

The main script is `watchdog.sc`. 
Provide user credentials and other information needed for the
registrations in a `local.cor.conf` file in this directory. 
Use `cor.conf` as a template. 
DO NOT commit the `local.cor.conf` file.

**Running**

Initially, capture the state of the files from both places
(COR and Github) by running:

    ./watchdog.sc refreshCorInfo
    ./watchdog.sc refreshGithubInfo

Just to see a report about any changed, new, or removed files
(and based on the currently captured information):
   
    ./watchdog.sc compare

To actually process the updates and reflect them into the COR:

    ./watchdog.sc run

This ios already functional (see initial output 
[here](https://gist.github.com/carueda/a2b781d653651c4ddec4b83c73e5cdb4)).

TODO: set cronjob or webhook to trigger this.
