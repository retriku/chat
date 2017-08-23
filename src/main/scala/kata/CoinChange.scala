package kata

object CoinChange {

  case class Result(denomination: Int, coinCount: Int)

}

trait CoinChange {
  /**
    * Find the most efficient (with least total coins) change for the 'expectedTotal' coins, using the coins provided
    * in 'denominations'. Assume that the amount of coins available as change in each denomination is infinite.
    *
    * @param denominations The coin 'denominations' available in the monetary system
    * @param expectedTotal The total amount of money we should obtain
    * @return Some of a Set of CoinChange.Result-s specifying how many coins of which nominations we should use. None if changing is not possible.
    */
  def change(denominations: Set[Int])(expectedTotal: Int): Option[Set[CoinChange.Result]]
}

object CoinChangeImpl
  extends App
    with CoinChange {

  import CoinChange._

  /**
    * Find the most efficient (with least total coins) change for the 'expectedTotal' coins, using the coins provided
    * in 'denominations'. Assume that the amount of coins available as change in each denomination is infinite.
    *
    * @param denominations The coin 'denominations' available in the monetary system
    * @param expectedTotal The total amount of money we should obtain
    * @return Some of a Set of CoinChange.Result-s specifying how many coins of which nominations we should use. None if changing is not possible.
    */
  override def change(denominations: Set[Int])
                     (expectedTotal: Int): Option[Set[Result]] = {
    val initialCoinChanges: Array[Set[Result]] = (0 until expectedTotal).map { i ⇒
      val amount = i + 1
      if (denominations.exists(amount % _ == 0)) {
        val denomination = denominations.filter(amount % _ == 0).max
        Set(Result(
          denomination = denomination,
          coinCount = amount / denomination))
      } else {
        Set.empty[Result]
      }
    }.toArray

    val results = (0 until expectedTotal).foldLeft(initialCoinChanges) {
      findAllMatches(denominations)
    }

    require(results.length == expectedTotal)

    results.filterNot(_.isEmpty).lastOption
  }

  private def findAllMatches(denominations: Set[Int]): (Array[Set[Result]], Int) ⇒ Array[Set[Result]] = {
    case (soFar, i) ⇒
      val amount = i + 1
      val j = if (i % 2 == 0) i / 2 else (i / 2) + 1
      val intervalStart = soFar.take(j)
      val intervalEnd = soFar.slice(i / 2, i).reverse
      val coinChanges = intervalStart.zip(intervalEnd).map(mergeAndNormalize).filter(amountMatches(amount)).sortBy {
        _.toList.map(_.coinCount).sum
      }.headOption

      coinChanges.map{ cc ⇒
        val currentMin = soFar(i).toList.map(_.coinCount).sum
        val newMin = cc.toList.map(_.coinCount).sum

        if (elementUpdateNeeded(
          currentMin = currentMin,
          newMin = newMin)) {
          soFar(i) = cc
          soFar
        } else {
          soFar
        }
      }.getOrElse(soFar)
  }

  private def amountMatches(amount: Int): PartialFunction[Set[Result], Boolean] = {
    case coinChanges ⇒
      coinChanges.map(d ⇒ d.coinCount * d.denomination).sum == amount
  }

  private def elementUpdateNeeded(currentMin: Int,
                                  newMin: Int) = {
    newMin < currentMin || currentMin == 0
  }

  def mergeAndNormalize: PartialFunction[(Set[Result], Set[Result]), Set[Result]] = {
    case (coinChanges1, coinChanges2) ⇒
      coinChanges1.flatMap { r1 ⇒
        coinChanges2.find(_.denomination == r1.denomination).map { r2 ⇒
          Set(r1.copy(coinCount = r1.coinCount + r2.coinCount))
        }.getOrElse {
          coinChanges2 + r1
        }
      }
  }

  val s = System.currentTimeMillis()
  val r1 = change(Set(1, 2, 5, 10, 20, 50, 100, 200, 500))(173)
  require(
    r1.map(_.toList.sortBy(_.denomination)) contains List(Result(1, 1), Result(2, 1), Result(20, 1), Result(50, 1),
      Result(100, 1)))
  require(change(Set(1, 3, 4))(8) contains Set(Result(4, 2)))
  require(change(Set(2, 3, 4))(6) contains Set(Result(3, 2)))
  require(change(Set(1, 7, 8))(6) contains Set(Result(1, 6)))
  require(change(Set(2, 3, 4))(1).isEmpty)
  require(change(Set())(5).isEmpty)
  require(change(Set())(0).isEmpty)
  require(change(Set(1, 2))(0).isEmpty)
  require(change(Set(1))(20000) contains Set(Result(1, 20000)))

  val e = System.currentTimeMillis()
  val sec = (e - s) / 1000
  println(s"elapsed: ${sec / 60}:${sec % 60}.${(e - s) % 1000}")
}
