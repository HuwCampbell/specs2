package org.specs2.mutable

class SpecSpec extends Spec {
  args(sequential = true)

  "A Spec specification contains very few implicits" >> {
    import scala.reflect.runtime.universe._
    typeOf[Spec].members.filter(m => m.isImplicit && m.isMethod).map(_.name.toString).toSet must
      beEqualTo(Set(
        "theValue",          // must expectations
        "blockExample",      // create >> examples
        "anyToArgProperty"   // to create arguments
      ))
  }

}