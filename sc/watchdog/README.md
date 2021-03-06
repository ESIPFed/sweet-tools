Code to check for updates in the ontologies at the Github repo
and perform automatic registration of new versions.

The main script is `watchdog.sc`.
Provide user credentials and other information needed for the
registrations in a `local.cor.conf` file in this directory.
Use `cor.conf` as a template.
DO NOT commit the `local.cor.conf` file.

**Running**

Initially, capture the state of the files from the COR:

    ./watchdog.sc refreshCorInfo

This state is basically a checksum (SHA-256) of the latest version of
each SWEET ontology file.

Just to see a report about any changed, new, or removed files at Github
wrt the currently captured information from COR:

    ./watchdog.sc compare

To actually reflect any updates in the COR:

    ./watchdog.sc update
    
NOTE: any error during a registration is just printed out but the process
is let to continue, in particular this means that the set of SHA-256
checksums will still be updated so the same registrations are not 
reattempted in a next run.  This logic is of course not very elegant/robust
but the case we want to handle at the moment is failures while updating 
the triple store, not error in the ontology registration itself.

Example output
[here](https://gist.github.com/carueda/a2b781d653651c4ddec4b83c73e5cdb4).

**Cronjob**

Looks basically like so on the COR machine:

    29 */6 * * *  cd /home/cor-admin1/sweet-tools/sc/watchdog/ && ./watchdog.sc update >> watchdog.cronjob.out



TODO:
- consider using a webhook to trigger the update (instead of cronjob)
- capture git commit message as log for ontology registration
- as a possible logic variation, capture latest commit from Github and
  use this as a basis to detect changes there
