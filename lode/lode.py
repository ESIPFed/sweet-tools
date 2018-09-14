# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os, errno
import requests
from bs4 import BeautifulSoup

try:
    os.makedirs('sweet_lode')
except OSError as e:
    if e.errno != errno.EEXIST:
        raise

r = requests.get("http://www.essepuntato.it/lode/owlapi/http://cor.esipfed.org/ont/api/v0/ont%3Firi=http://sweetontology.net/sweetAll")
sweet_all_html = BeautifulSoup(r.content)

lode_urls = []
for link in sweet_all_html.find_all('a'):
    if 'http://www.essepuntato.it/lode/owlapi/' in str(link):
        href_content = link.get('href')
        #First augment the HTML to accomodate the cached LODE content
        cache_tag = sweet_all_html.new_tag("a")
        cache_tag['href'] = "./" + href_content.rsplit('/', 1)[-1] + '.html'
        cache_tag.string = "visualise cached LODE version -"
        link.insert_before(cache_tag)
        #Now, cache the LODE URLs for fetching.
        lode_urls.append(href_content)

with open('sweet_lode/sweetAll.html', 'w') as f:
    f.write(str(sweet_all_html.html))
    f.close()

for url in lode_urls:
    html = requests.get(str(url))
    with open('sweet_lode/' + url.rsplit('/', 1)[-1] + '.html', 'w') as f:
        f.write(str(html.html))
        f.close()