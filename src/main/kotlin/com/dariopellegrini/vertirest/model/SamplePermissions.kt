package com.dariopellegrini.vertirest.model

import com.dariopellegrini.vertirest.vertirest.constants.RESTPermission
import com.dariopellegrini.vertirest.vertirest.permission.PermissionsDescriptor

internal class SamplePermissions : PermissionsDescriptor {
    override val guestPermission: Map<RESTPermission, Boolean>
        get() = mapOf(RESTPermission.READ to true,
                RESTPermission.READ_PUBLIC to true,
                RESTPermission.CREATE to true,
                RESTPermission.UPDATE to false,
                RESTPermission.DELETE to false)

    override val userPermission: Map<RESTPermission, Boolean>
        get() = mapOf(RESTPermission.READ to true,
                RESTPermission.READ_PUBLIC to true,
                RESTPermission.CREATE to true,
                RESTPermission.UPDATE to false,
                RESTPermission.DELETE to false)

    override val ownerPermission: Map<RESTPermission, Boolean>
        get() = mapOf(RESTPermission.READ to true,
                RESTPermission.READ_PUBLIC to true,
                RESTPermission.CREATE to true,
                RESTPermission.UPDATE to true,
                RESTPermission.DELETE to true)
}