package scalaz

////
/**
 * A type safe alternative to universal equality (`scala.Any#==`).
 *
 * @see [[scalaz.Equal.EqualLaw]]
 */
////
trait Equal[F]  { self =>
  ////
  def equal(a1: F, a2: F): Boolean

  def contramap[G](f: G => F): Equal[G] = new Equal[G] {
    def equal(a1: G, a2: G) = self.equal(f(a1), f(a2))
  }

  /** @return true, if `equal(f1, f2)` is known to be equivalent to `f1 == f2` */
  def equalIsNatural: Boolean = false

  // derived functions

  trait EqualLaw {
    import std.boolean.conditional
    def commutative(f1: F, f2: F): Boolean = equal(f1, f2) == equal(f2, f1)
    def reflexive(f: F): Boolean = equal(f, f)
    def transitive(f1: F, f2: F, f3: F): Boolean = {
      conditional(equal(f1, f2) && equal(f2, f3), equal(f1, f3))
    }
    def naturality(f1: F, f2: F): Boolean = {
      conditional(equalIsNatural, equal(f1, f2) == (f1 == f2))
    }
  }
  def equalLaw = new EqualLaw {}
  ////
  val equalSyntax = new scalaz.syntax.EqualSyntax[F] { def F = Equal.this }
}

object Equal {
  @inline def apply[F](implicit F: Equal[F]): Equal[F] = F

  ////
  /** Creates an Equal instance based on universal equality, `a1 == a2` */
  def equalA[A]: Equal[A] = new Equal[A] {
    def equal(a1: A, a2: A): Boolean = a1 == a2
    override def equalIsNatural: Boolean = true
  }

  /** Creates an Equal instance based on reference equality, `a1 eq a2` */
  def equalRef[A <: AnyRef]: Equal[A] = new Equal[A] {
    def equal(a1: A, a2: A): Boolean = a1 eq a2
  }

  def equalBy[A, B: Equal](f: A => B): Equal[A] = Equal[B] contramap f

  // scalaz-deriving provides a coherent n-arity extension
  private[scalaz] class EqualDerives extends ContravariantDerives[Equal] {
    override def divide2[A1, A2, Z](a1: =>Equal[A1], a2: =>Equal[A2])(
      f: Z => (A1, A2)
    ): Equal[Z] = { (z1, z2) =>
      val (s1, s2) = f(z1)
      val (t1, t2) = f(z2)
      ((s1.asInstanceOf[AnyRef] eq t1.asInstanceOf[AnyRef]) || a1.equal(s1, t1)) &&
      ((s2.asInstanceOf[AnyRef] eq t2.asInstanceOf[AnyRef]) || a2.equal(s2, t2))
    }
    override def conquer[A]: Equal[A] = ((_, _) => true)

    override def codivide1[Z, A1](a1: =>Equal[A1])(f: Z => A1): Equal[Z] =
      ((z1, z2) => a1.equal(f(z1), f(z2)))

    override def codivide2[Z, A1, A2](a1: =>Equal[A1], a2: =>Equal[A2])(
      f: Z => A1 \/ A2
    ): Equal[Z] = { (z1, z2) =>
      (f(z1), f(z2)) match {
        case (-\/(s), -\/(t)) => (s.asInstanceOf[AnyRef] eq t.asInstanceOf[AnyRef]) || a1.equal(s, t)
        case (\/-(s), \/-(t)) => (s.asInstanceOf[AnyRef] eq t.asInstanceOf[AnyRef]) || a2.equal(s, t)
        case _                => false
      }
    }
  }
  implicit val Derives: Derives[Equal] = new EqualDerives

  /** Construct an instance, but prefer SAM types with scala 2.12+ */
  def equal[A](f: (A, A) => Boolean): Equal[A] = new Equal[A] {
    def equal(a1: A, a2: A) = f(a1, a2)
  }

  ////
}
