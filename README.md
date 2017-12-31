# solr-quantities-detection-qparsers

Welcome, Solr explorer!   
This is a repository containing a set of Solr add-ons for detecting and managing quantities within query strings.

The concept around a quantity has been nicely described here [1] and here [2], by Martin Fowler.     
A dimensioned quantity can be seen as a pair of an amount and a unit (such as "thirty meters" or "two kilograms", or 267 dollars - yes, a money is a case of dimensioned quantity)     

![Quantity (Martin Fowler)](http://www.dsc.ufcg.edu.br/~jacques/cursos/map/recursos/fowler-ap/Analysis%20Pattern%20Quantity_files/quantityClass.gif)

Sometimes, quantities have a special meaning within an application context, especially in a search application like e-commerce shops where products are identified by dimensions. The first example that comes in my mind? 

![Beers](https://cdn-s3.si.com/s3fs-public/2017/07/21/beer-banner.jpg)

As you can imagine, beer is offered in several containers (e.g. cans, bottles); each of them is available in several sizes (e.g. 25cl, 50cl, 75cl). A good catalog would capture these information in dedicated fields, like "container" (bottle, can) and "capacity" (25cl, 50cl, 75cl in the example above) so the search logic can properly use them for faceting and narrowing.  

Faceting (and subsequent narrowing) are good examples of what you can do *after* the first / top search has been executed: you can filter by facets in order to refine search results and hopefully find what you was looking for. Fields like "container" and "capacity" are good candidates for filtering, because they are structured fields, they domain is well defined and delimited. 

But if we start from the beginning of the story, we don't have any search result at all: there's only a blank textfield where the user is going to type something. This "something" could be whatever, anything (in his mind) which is related with the product he's looking for: a brand, a container type, a model name, a quantity which represents a relevant feature of the product, or a combination of all of them.     

The main idea behind this "quantity" plugin is the following: starting from the user entered query, it detects the quantities (i.e. the amounts and the corresponding units). Then, these information will be isolated from the main query and they will be used for boosting up those products "matching" those quantities. The double quotes are because the match can be configured in different ways:

* **exact match**: all bottles with a capacity of 25cl 
* **range match**: all bottles with a capacity between 50cl and 75cl.
* **equivalence exact match**: all bottles with a capacity of 0.5 litre (1lt = 100cl)               
* **equivalence range match**: all bottles with a capacity between 0.5 and 1 litre (1lt = 100cl)               
 
***

[1] https://martinfowler.com/books/ap.html    
[2] http://www.dsc.ufcg.edu.br/~jacques/cursos/map/recursos/fowler-ap/Analysis%20Pattern%20Quantity.htm
