package com.nike.cerberus.api

import com.thedeanda.lorem.Lorem
import io.restassured.path.json.JsonPath
import org.apache.commons.lang3.RandomStringUtils
import org.jboss.aerogear.security.otp.Totp

import static org.junit.Assert.assertEquals
import static com.nike.cerberus.api.CerberusApiActions.*
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class CerberusCompositeApiActions {
    private CerberusCompositeApiActions() {}

    static final String ROOT_INTEGRATION_TEST_SDB_PATH = "app/cerberus-integration-tests-sdb"

    static void "create, read, update then delete a secret node"(String cerberusAuthToken) {
        def path = "${ROOT_INTEGRATION_TEST_SDB_PATH}/${UUID.randomUUID().toString()}"
        String value1 = 'value1'
        String value2 = 'value2'

        // Create the initial secret node
        createOrUpdateSecretNode([value: value1], path, cerberusAuthToken)
        // Read and verify that it was created
        def resp = readSecretNode(path, cerberusAuthToken)
        assertEquals(value1, resp?.'data'?.'value')
        // Update the secret node
        createOrUpdateSecretNode([value: value2], path, cerberusAuthToken)
        // Read that the node was updated
        def resp2 = readSecretNode(path, cerberusAuthToken)
        assertEquals(value2, resp2?.'data'?.'value')
        // Delete the node
        deleteSecretNode(path, cerberusAuthToken)
        // Verify that the node was deleted
        assertThatSecretNodeDoesNotExist(path, cerberusAuthToken)
    }

    static void "v1 create, read, list, update and then delete a safe deposit box"(Map cerberusAuthPayloadData) {
        String cerberusAuthToken = cerberusAuthPayloadData.'client_token'
        String groups = cerberusAuthPayloadData.metadata.groups
        def group = groups.split(/,/)[0]

        // Create a map of category ids to names'
        JsonPath getCategoriesResponse = getCategories(cerberusAuthToken)
        def catMap = [:]
        getCategoriesResponse.getList("").each { category ->
            catMap.put category.display_name, category.id
        }
        // Create a map of role ids to names
        JsonPath getRolesResponse = getRoles(cerberusAuthToken)
        def roleMap = [:]
        getRolesResponse.getList("").each { role ->
            roleMap.put role.name, role.id
        }

        String name = "${RandomStringUtils.randomAlphabetic(5,10)} ${RandomStringUtils.randomAlphabetic(5,10)}"
        String description = "${Lorem.getWords(50)}"
        String categoryId = catMap.Applications
        String owner = group
        def userGroupPermissions = [
            [
                "name": 'foo',
                "role_id": roleMap.read
            ]
        ]
        def iamRolePermissions = [
            [
                "account_id": "1111111111",
                "iam_role_name": "fake_role",
                "role_id": roleMap.write
            ]
        ]

        def sdbId = createSdbV1(cerberusAuthToken, name, description, categoryId, owner, userGroupPermissions, iamRolePermissions)
        JsonPath sdb = readSdb(cerberusAuthToken, sdbId, V1_SAFE_DEPOSIT_BOX_PATH)

        // verify that the sdb we created contains the data we expect
        assertSafeDepositBoxHasFields(sdb, name, description, categoryId, owner, userGroupPermissions, iamRolePermissions)

        // verify that the listing call contains our new SDB
        def sdbList = listSdbs(cerberusAuthToken, V1_SAFE_DEPOSIT_BOX_PATH)
        def foundNewSdb = false
        def listSdb

        sdbList.getList("").each { sdbMeta ->
            if (sdbMeta.id == sdbId) {
                foundNewSdb = true
                listSdb = sdbMeta
            }
        }
        assertTrue("Failed to find the newly created SDB in the list results", foundNewSdb)
        assertEquals(listSdb.name, sdb.get('name'))
        assertEquals(listSdb.id, sdb.get('id'))
        assertEquals(listSdb.path, sdb.get('path'))
        assertEquals(listSdb.'category_id', sdb.get('category_id'))

        // update the sdb
        description = "${Lorem.getWords(60)}"
        userGroupPermissions.add([
            "name": 'bar',
            "role_id": roleMap.write
        ])
        iamRolePermissions.add([
            "account_id": "1111111111",
            "iam_role_name": "fake_role2",
            "role_id": roleMap.read
        ])
        updateSdbV1(cerberusAuthToken, sdbId, description, owner, userGroupPermissions, iamRolePermissions)
        JsonPath sdbUpdated = readSdb(cerberusAuthToken, sdbId, V1_SAFE_DEPOSIT_BOX_PATH)

        // verify that the sdbUpdated we created contains the data we expect
        assertSafeDepositBoxHasFields(sdbUpdated, name, description, categoryId, owner, userGroupPermissions, iamRolePermissions)

        // delete the SDB
        deleteSdb(cerberusAuthToken, sdbId, V1_SAFE_DEPOSIT_BOX_PATH)

        // verify that the sdb is not longer in the list
        def updatedSdbList = listSdbs(cerberusAuthToken, V1_SAFE_DEPOSIT_BOX_PATH)
        def isSdbPresentInUpdatedList = false

        updatedSdbList.getList("").each { sdbMeta ->
            if (sdbMeta.id == sdbId) {
                isSdbPresentInUpdatedList = true
            }
        }
        assertFalse("The created sdb should not be in the sdb listing call after deleting it", isSdbPresentInUpdatedList)
    }

    static void "v2 create, read, list, update and then delete a safe deposit box"(Map cerberusAuthPayloadData) {
        String cerberusAuthToken = cerberusAuthPayloadData.'client_token'
        String groups = cerberusAuthPayloadData.metadata.groups
        def group = groups.split(/,/)[0]

        // Create a map of category ids to names'
        JsonPath getCategoriesResponse = getCategories(cerberusAuthToken)
        def catMap = [:]
        getCategoriesResponse.getList("").each { category ->
            catMap.put category.display_name, category.id
        }
        // Create a map of role ids to names
        JsonPath getRolesResponse = getRoles(cerberusAuthToken)
        def roleMap = [:]
        getRolesResponse.getList("").each { role ->
            roleMap.put role.name, role.id
        }

        String name = "${RandomStringUtils.randomAlphabetic(5,10)} ${RandomStringUtils.randomAlphabetic(5,10)}"
        String description = "${Lorem.getWords(50)}"
        String categoryId = catMap.Applications
        String owner = group
        def userGroupPermissions = [
            [
                "name": 'foo',
                "role_id": roleMap.read
            ]
        ]
        def iamRolePermissions = [
            [
                "iam_principal_arn": "arn:aws:iam::1111111111:role/fake_role",
                "role_id": roleMap.write
            ]
        ]

        // verify that the sdb we created contains the data we expect
        def createdSdb = createSdbV2(cerberusAuthToken, name, description, categoryId, owner, userGroupPermissions, iamRolePermissions)
        assertSafeDepositBoxHasFields(createdSdb, name, description, categoryId, owner, userGroupPermissions, iamRolePermissions)

        // test read sdb returns returns expected data
        def sdbId = createdSdb.getString("id")
        JsonPath sdb = readSdb(cerberusAuthToken, sdbId, V2_SAFE_DEPOSIT_BOX_PATH)
        assertSafeDepositBoxHasFields(sdb, name, description, categoryId, owner, userGroupPermissions, iamRolePermissions)

        // verify that the listing call contains our new SDB
        def sdbList = listSdbs(cerberusAuthToken, V2_SAFE_DEPOSIT_BOX_PATH)
        def foundNewSdb = false
        def listSdb

        sdbList.getList("").each { sdbMeta ->
            if (sdbMeta.id == sdbId) {
                foundNewSdb = true
                listSdb = sdbMeta
            }
        }
        assertTrue("Failed to find the newly created SDB in the list results", foundNewSdb)
        assertEquals(listSdb.name, sdb.get('name'))
        assertEquals(listSdb.id, sdb.get('id'))
        assertEquals(listSdb.path, sdb.get('path'))
        assertEquals(listSdb.'category_id', sdb.get('category_id'))

        // update the sdb
        description = "${Lorem.getWords(60)}"
        userGroupPermissions.add([
            "name": 'bar',
            "role_id": roleMap.write
        ])
        iamRolePermissions.add([
            "iam_principal_arn": "arn:aws:iam::1111111111:role/fake_role2",
            "role_id": roleMap.read
        ])
        JsonPath sdbUpdatedUpdate = updateSdbV2(cerberusAuthToken, sdbId, description, owner, userGroupPermissions, iamRolePermissions)

        // verify that the sdbUpdated we created contains the data we expect
        assertSafeDepositBoxHasFields(sdbUpdatedUpdate, name, description, categoryId, owner, userGroupPermissions, iamRolePermissions)

        JsonPath sdbUpdatedRead = readSdb(cerberusAuthToken, sdbId, V2_SAFE_DEPOSIT_BOX_PATH)
        assertSafeDepositBoxHasFields(sdbUpdatedRead, name, description, categoryId, owner, userGroupPermissions, iamRolePermissions)

        // delete the SDB
        deleteSdb(cerberusAuthToken, sdbId, V2_SAFE_DEPOSIT_BOX_PATH)

        // verify that the sdb is not longer in the list
        def updatedSdbList = listSdbs(cerberusAuthToken, V2_SAFE_DEPOSIT_BOX_PATH)
        def isSdbPresentInUpdatedList = false

        updatedSdbList.getList("").each { sdbMeta ->
            if (sdbMeta.id == sdbId) {
                isSdbPresentInUpdatedList = true
            }
        }
        assertFalse("The created sdb should not be in the sdb listing call after deleting it", isSdbPresentInUpdatedList)
    }

    static Map "login user with multi factor authentication (or skip mfa if not required) and return auth data"(
            String username, String password, String otpSecret, String deviceId) {

        JsonPath loginResp = loginUser(username, password)
        String status = loginResp.getString("status")
        if (status == "success") {
            return loginResp.getString("data.client_token")
        } else {
            def mfaResp = finishMfaUserAuth(
                    loginResp.getString("data.state_token"),
                    deviceId,
                    new Totp(otpSecret).now())

            return mfaResp.get('data.client_token')
        }
    }

    private static boolean iamRolePermissionEquals(def iamRolePermission1, def iamRolePermission2) {
        if (iamRolePermission1.'account_id' == iamRolePermission2.'account_id' &&
                iamRolePermission1.'iam_role_name' == iamRolePermission2.'iam_role_name' &&
                iamRolePermission1.'iam_principal_arn' == iamRolePermission2.'iam_principal_arn') {

            return true
        }

        return false
    }

    private static void assertIamRolePermissionsEquals(def expectedIamRolePermissions, def actualIamRolePermissions) {
        assertEquals(expectedIamRolePermissions.size(), actualIamRolePermissions.size())
        for (def expectedPerm : expectedIamRolePermissions) {
            def found = false
            for (def actualPerm : actualIamRolePermissions) {
                if (expectedPerm.'iam_role_name' == actualPerm.'iam_role_name' &&
                        expectedPerm.'iam_principal_arn' == actualPerm.'iam_principal_arn') {
                    found = true
                    assertTrue(iamRolePermissionEquals(expectedPerm, actualPerm))
                }
            }
            assertTrue("The expected user permission was not found in the actual results", found)
        }
    }

    private static void assertUserGroupPermissionsEquals(def expectedUserGroupPermissions, def actualUserGroupPermissions) {

        assertEquals(expectedUserGroupPermissions.size(), actualUserGroupPermissions.size())
        for (def expectedPerm : expectedUserGroupPermissions) {
            def found = false
            for (def actualPerm : actualUserGroupPermissions) {
                if (expectedPerm.name == actualPerm.name) {
                    found = true
                    assertEquals(expectedPerm.'role_id', actualPerm.'role_id')
                    assertEquals(expectedPerm.name, actualPerm.name)
                }
            }
            assertTrue("The expected user permission was not found in the actual results", found)
        }
    }

    private static void assertSafeDepositBoxHasFields(def safeDepositBox, def name, def description, def categoryId,
                                                      def owner, def userGroupPermissions, def iamRolePermissions) {

        assertEquals(name, safeDepositBox.get('name'))
        assertEquals(description, safeDepositBox.get('description'))
        assertEquals(categoryId, safeDepositBox.get('category_id'))
        assertEquals(owner, safeDepositBox.get('owner'))
        assertIamRolePermissionsEquals(iamRolePermissions, safeDepositBox.getList('iam_role_permissions'))
        assertUserGroupPermissionsEquals(userGroupPermissions, safeDepositBox.getList('user_group_permissions'))
    }
}
