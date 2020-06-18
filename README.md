# VertIV
### Vert.x Improved Variation
This project is a (pretty thin so far) wrapper of utilities, patterns and likely dependencies
for a more streamlined vert.x microservice creation experience. 

### VertIV is opinionated
Just as a heads-up.
* The target language is groovy,
  though you probably can use it from within Java if you don't mind the overhead of this dynamic 
  language.
* Uses rxjava3. And you should, too. Don't be a b...uffer

### Build

``mvn install`` seems to not blow up in a huge nuclear explosion, so try running that.

### Test

Through IntelliJ IDEA: tests run fine.

Through ``mvn``: currently broken(TODO!)