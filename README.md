# imglib2-android-neighborhood

### Sample app based on Example4b

This app is made for knowing how to use imglib2 on android.

imglib2 has dependencies on scijava and scifio which needs javax or awt,
So I have just copied source trees under the app repo and deleted few functions.
More specifically imglib2 codes are fetched at [057275](https://github.com/imglib/imglib2/tree/0572755ef7e1927f3207a22452182b35d1aa5409)
and imglib2-algorithm codes are at [d27c9b](https://github.com/imglib/imglib2-algorithm/tree/d27c9bd57ecc72b665a41030f8264c0b2b68d993)

My target of checking is "Neighborhood" methods on imglib2-algorithm and it seems working nicely.

### Requirements

Few source code on imglib2 needs java8 syntax, so this app needs to build on Android-Studio-3.0c3 or above.