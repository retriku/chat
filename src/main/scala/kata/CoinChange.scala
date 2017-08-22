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
    val initialCoinChanges: List[Set[Result]] = (0 until expectedTotal).map(_ ⇒ Set.empty[Result]).toList

    val results = (0 until expectedTotal).foldLeft(initialCoinChanges) {
      findAllMatches(denominations)
    }

    require(results.size == expectedTotal)

    results.filterNot(_.isEmpty).filter(amountMatches(expectedTotal)).sortBy(
      _.map(_.coinCount).sum).headOption
  }

  private def findAllMatches(denominations: Set[Int]): (List[Set[Result]], Int) ⇒ List[Set[Result]] = {
    case (soFar, i) ⇒
      val amount = i + 1
      if (denominations.contains(amount)) {
        soFar.take(i) ::: List(Set(
          Result(
            denomination = amount,
            coinCount = 1))) ::: soFar.drop(i + 1)
      } else if (denominations.filter(1 < _).exists(amount % _ == 0)) {
        val denomination = denominations.filter(1 < _).filter(amount % _ == 0).max
        soFar.take(i) ::: List(
          Set(Result(
            denomination = denomination,
            coinCount = amount / denomination))
        ) ::: soFar.drop(i + 1)
      } else {
        val j = if (i % 2 == 0) i / 2 else (i / 2) + 1
        val intervalStart = soFar.take(j)
        val intervalEnd = soFar.slice(i / 2, i).reverse
        val coinChanges = intervalStart.zip(intervalEnd).map {
          case (coinChanges1, coinChanges2) ⇒
            val normal = mergeAndNormalize(
              coinChanges1 = coinChanges1,
              coinChanges2 = coinChanges2)
            normal
        }.filter(amountMatches(amount)).sortBy(
          _.map(_.coinCount).sum).headOption.toList
        val currentMin = soFar(i).map(_.coinCount).sum
        val newMin = coinChanges.map(_.map(_.coinCount).sum)

        if (elementUpdateNeeded(
          coinChanges = coinChanges,
          currentMin = currentMin,
          newMin = newMin)) {
          soFar.take(i) ::: coinChanges ::: soFar.drop(i + 1)
        }
        else {
          soFar
        }
      }
  }

  private def amountMatches(amount: Int): PartialFunction[Set[Result], Boolean] = {
    case coinChanges ⇒
      coinChanges.map(d ⇒ d.coinCount * d.denomination).sum == amount
  }

  private def elementUpdateNeeded(coinChanges: List[Set[Result]],
                                  currentMin: Int,
                                  newMin: List[Int]) = {
    (newMin.exists(currentMin > _) || currentMin == 0) && coinChanges.nonEmpty
  }

  def mergeAndNormalize(coinChanges1: Set[Result],
                        coinChanges2: Set[Result]): Set[Result] = {
    coinChanges1.flatMap { r1 ⇒
      coinChanges2.find(_.denomination == r1.denomination).map { r2 ⇒
        Set(r1.copy(coinCount = r1.coinCount + r2.coinCount))
      }.getOrElse {
        coinChanges2 + r1
      }
    }
  }

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
}