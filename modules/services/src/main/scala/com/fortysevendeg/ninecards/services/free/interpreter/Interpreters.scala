package com.fortysevendeg.ninecards.services.free.interpreter

import cats.~>
import com.fortysevendeg.ninecards.services.free.algebra.AppGooglePlay._
import com.fortysevendeg.ninecards.services.free.algebra.AppPersistence._
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollectionSubscriptions._
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollections._
import com.fortysevendeg.ninecards.services.free.algebra.Users._
import com.fortysevendeg.ninecards.services.free.domain._
import com.fortysevendeg.ninecards.services.free.interpreter.impl.AppGooglePlayImpl._
import com.fortysevendeg.ninecards.services.free.interpreter.impl.AppPersistenceImpl._
import com.fortysevendeg.ninecards.services.free.interpreter.impl.SharedCollectionPersistenceImpl._
import com.fortysevendeg.ninecards.services.free.interpreter.impl.SharedCollectionSubscriptionPersistenceImpl._
import com.fortysevendeg.ninecards.services.free.interpreter.impl.UserPersistenceImpl._

import scalaz.concurrent.Task

object Interpreters {

  object AppGooglePlayInterpreter extends (AppGooglePlayOps ~> Task) {
    def apply[A](fa: AppGooglePlayOps[A]) = fa match {
      case GetCategoriesFromGooglePlay(packageNames: Seq[String]) =>
        Task {
          appGooglePlayImpl.getCategoriesFromGooglePlay(packageNames)
        }
    }
  }

  object AppPersistenceInterpreter extends (AppPersistenceOps ~> Task) {
    def apply[A](fa: AppPersistenceOps[A]) = fa match {
      case GetCategories(packageNames: Seq[String]) =>
        Task {
          appPersistenceImpl.getCategories(
            packageNames = packageNames)
        }
      case SaveCategories(packageNames: Seq[String]) =>
        Task {
          appPersistenceImpl.saveCategories(packageNames)
        }
    }
  }

  object SharedCollectionInterpreter extends (SharedCollectionOps ~> Task) {

    def apply[A](fa: SharedCollectionOps[A]) = fa match {
      case AddSharedCollection(collection: SharedCollection) =>
        Task {
          sharedCollectionPersistenceImpl.addSharedCollection(collection)
        }
      case GetSharedCollectionById(collectionId: String) =>
        Task {
          sharedCollectionPersistenceImpl.getSharedCollectionById(collectionId)
        }
      case UpdateInstallNotification(collectionId: String) =>
        Task {
          sharedCollectionPersistenceImpl.updateInstallNotification(collectionId)
        }
      case UpdateViewNotification(collectionId: String) =>
        Task {
          sharedCollectionPersistenceImpl.updateViewNotification(collectionId)
        }
    }
  }

  object SharedCollectionSubscriptionInterpreter extends (SharedCollectionSubscriptionOps ~> Task) {

    def apply[A](fa: SharedCollectionSubscriptionOps[A]) = fa match {
      case AddSharedCollectionSubscription(collectionId: String) =>
        Task {
          sharedCollectionSubscriptionPersistenceImpl.addSharedCollectionSubscription(
            sharedCollectionId = collectionId)
        }
      case DeleteSharedCollectionSubscription(collectionId: String) =>
        Task {
          sharedCollectionSubscriptionPersistenceImpl.deleteSharedCollectionSubscription(
            sharedCollectionId = collectionId)
        }
    }
  }

  object UserInterpreter extends (UserOps ~> Task) {

    def apply[A](fa: UserOps[A]) = fa match {
      case AddUser(user: User) =>
        Task {
          userPersistenceImpl.addUser(user)
        }
      case GetUserByUserName(username: String) =>
        Task {
          userPersistenceImpl.getUserByUserName(username)
        }
      case CheckPassword(pass: String) =>
        Task {
          userPersistenceImpl.checkPassword(pass)
        }
    }
  }

}