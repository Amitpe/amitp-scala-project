import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class MyClassTest extends SpecificationWithJUnit {

  "MyClass" should {

    "do this" in new Context {
      myClass.foo() mustEqual "foo"
    }

    "do that" in new Context {
      ko
    }.pendingUntilFixed("Not implemented yet")

  }

}

trait Context extends Scope {
  val a = "bla"
  val myClass = new MyClass
}
