/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package secure

import models.security._

/**
 * A trait to define Authorization objects that let you hook
 * an authorization implementation in SecuredActions
 *
 */
trait Authorization {
  /**
   * Checks whether the user is authorized to execute an action or not.
   *
   * @param user
   * @return
   */
  def isAuthorized(user: User): Boolean
}
