package cards.nine.services.persistence

import java.sql.Timestamp
import java.time.Instant

import cats.Monad
import cats.syntax.traverse._
import cats.std.list
import cards.nine.services.free.domain.rankings._
import cards.nine.services.free.domain.{ Category, PackageName }
import cards.nine.services.persistence.NineCardsGenEntities._
import cards.nine.services.persistence.SharedCollectionPersistenceServices._
import cards.nine.services.persistence.UserPersistenceServices.UserData
import doobie.imports.ConnectionIO
import enumeratum.{ Enum, EnumEntry }
import org.scalacheck.{ Arbitrary, Gen }
import scalaz.std.list.listInstance

object NineCardsGenEntities {

  case class ApiKey(value: String) extends AnyVal

  case class DeviceToken(value: String) extends AnyVal

  case class Email(value: String) extends AnyVal

  case class SessionToken(value: String) extends AnyVal

  case class AndroidId(value: String) extends AnyVal

  case class PublicIdentifier(value: String) extends AnyVal

  case class IsoCode2(value: String) extends AnyVal

  case class IsoCode3(value: String) extends AnyVal

  case class WrongIsoCode2(value: String) extends AnyVal

  case class WrongIsoCode3(value: String) extends AnyVal
}

trait NineCardsScalacheckGen {

  def nonEmptyString(maxSize: Int) =
    Gen.resize(
      s = maxSize,
      g = Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
    )

  def fixedLengthString(size: Int) = Gen.listOfN(size, Gen.alphaChar).map(_.mkString)

  def fixedLengthNumericString(size: Int) = Gen.listOfN(size, Gen.numChar).map(_.mkString)

  val stringGenerator = Arbitrary.arbitrary[String]

  val emailGenerator: Gen[String] = for {
    mailbox ← nonEmptyString(50)
    topLevelDomain ← nonEmptyString(45)
    domain ← fixedLengthString(3)
  } yield s"$mailbox@$topLevelDomain.$domain"

  val timestampGenerator: Gen[Timestamp] = Gen.choose(0l, 253402300799l) map { seconds ⇒
    Timestamp.from(Instant.ofEpochSecond(seconds))
  }

  val sharedCollectionDataGenerator: Gen[SharedCollectionData] = for {
    publicIdentifier ← Gen.uuid
    publishedOn ← timestampGenerator
    author ← stringGenerator
    name ← stringGenerator
    installations ← Gen.posNum[Int]
    views ← Gen.posNum[Int]
    category ← nonEmptyString(64)
    icon ← nonEmptyString(64)
    community ← Gen.oneOf(true, false)
  } yield SharedCollectionData(
    publicIdentifier = publicIdentifier.toString,
    userId           = None,
    publishedOn      = publishedOn,
    author           = author,
    name             = name,
    installations    = installations,
    views            = views,
    category         = category,
    icon             = icon,
    community        = community
  )

  val userDataGenerator: Gen[UserData] = for {
    email ← emailGenerator
    apiKey ← Gen.uuid
    sessionToken ← Gen.uuid
  } yield UserData(email, apiKey.toString, sessionToken.toString)

  implicit val abAndroidId: Arbitrary[AndroidId] = Arbitrary(Gen.uuid.map(u ⇒ AndroidId(u.toString)))

  implicit val abApiKey: Arbitrary[ApiKey] = Arbitrary(Gen.uuid.map(u ⇒ ApiKey(u.toString)))

  implicit val abDeviceToken: Arbitrary[DeviceToken] = Arbitrary(Gen.uuid.map(u ⇒ DeviceToken(u.toString)))

  implicit val abEmail: Arbitrary[Email] = Arbitrary(emailGenerator.map(Email.apply))

  implicit val abIsoCode2: Arbitrary[IsoCode2] = Arbitrary(fixedLengthString(2).map(IsoCode2.apply))

  implicit val abIsoCode3: Arbitrary[IsoCode3] = Arbitrary(fixedLengthString(3).map(IsoCode3.apply))

  implicit val abPublicIdentifier: Arbitrary[PublicIdentifier] = Arbitrary(Gen.uuid.map(u ⇒ PublicIdentifier(u.toString)))

  implicit val abSessionToken: Arbitrary[SessionToken] = Arbitrary(Gen.uuid.map(u ⇒ SessionToken(u.toString)))

  implicit val abSharedCollectionData: Arbitrary[SharedCollectionData] = Arbitrary(sharedCollectionDataGenerator)

  implicit val abUserData: Arbitrary[UserData] = Arbitrary(userDataGenerator)

  implicit val abWrongIsoCode2: Arbitrary[WrongIsoCode2] = Arbitrary(fixedLengthNumericString(2).map(WrongIsoCode2.apply))

  def genEnumeratum[C <: EnumEntry](e: Enum[C]): Gen[C] =
    for (i ← Gen.choose(0, e.values.length - 1)) yield e.values(i)

  def abEnumeratum[C <: EnumEntry](e: Enum[C]): Arbitrary[C] = Arbitrary(genEnumeratum(e))

  val genPackage: Gen[PackageName] =
    Gen.nonEmptyListOf(Gen.alphaNumChar).map(chars ⇒ PackageName(chars.mkString))

  implicit val abCategory: Arbitrary[Category] = abEnumeratum[Category](Category)

  implicit val abCountry: Arbitrary[Country] = abEnumeratum[Country](Country)

  implicit val abContinent: Arbitrary[Continent] = abEnumeratum[Continent](Continent)

  val genGeoScope: Gen[GeoScope] = {
    val countries = Country.values.toSeq map CountryScope.apply
    val continents = Continent.values.toSeq map ContinentScope.apply
    Gen.oneOf(countries ++ continents ++ Seq(WorldScope))
  }

  implicit val abGeoScope: Arbitrary[GeoScope] = Arbitrary(genGeoScope)

  private[this] val genMonad: Monad[Gen] = new Monad[Gen] {
    def pure[A](a: A): Gen[A] = Gen.const(a)
    def flatMap[A, B](fa: Gen[A])(f: A ⇒ Gen[B]): Gen[B] = fa flatMap f
  }

  private[this] def listOfDistinctN[A](min: Int, max: Int, gen: Gen[A]): Gen[List[A]] =
    for {
      num ← Gen.choose(min, max)
      elems ← Gen.listOfN(num, gen)
    } yield elems.distinct

  val genRankingEntries: Gen[List[Entry]] = {

    def genCatEntries(cat: Category): Gen[List[Entry]] =
      for /*Gen*/ {
        packs ← listOfDistinctN(0, 10, genPackage)
        entries = packs.zipWithIndex map {
          case (pack, ind) ⇒ Entry(pack, cat, ind + 1)
        }
      } yield entries

    for /*Gen */ {
      cats ← listOfDistinctN(0, 10, genEnumeratum[Category](Category))
      entries ← list.listInstance.traverse(cats)(genCatEntries)(genMonad)
    } yield entries.flatten
  }

  implicit val abRankingEntries: Arbitrary[List[Entry]] = Arbitrary(genRankingEntries)

  def genCatRanking(maxSize: Int): Gen[CategoryRanking] =
    listOfDistinctN(0, maxSize, genPackage).map(CategoryRanking.apply)

  val genRanking: Gen[Ranking] =
    for {
      cats ← listOfDistinctN(0, 10, genEnumeratum[Category](Category))
      pairs ← list.listInstance.traverse(cats)({ cat ⇒
        for (r ← genCatRanking(10)) yield (cat, r)
      })(genMonad)
    } yield Ranking(pairs.toMap)

  implicit val abRanking: Arbitrary[Ranking] = Arbitrary(genRanking)

  val genDeviceApp: Gen[UnrankedApp] = genPackage map (p ⇒ UnrankedApp(p.name))

  implicit val abDeviceApp: Arbitrary[UnrankedApp] = Arbitrary(genDeviceApp)

}
