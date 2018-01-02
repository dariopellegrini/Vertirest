package com.dariopellegrini.vertirest.vertirest.permission

import com.dariopellegrini.vertirest.vertirest.constants.RESTPermission

class PermissionsContainer(val ownerPermission: Map<RESTPermission, Boolean>,
                           val userPermission: Map<RESTPermission, Boolean>,
                           val guestPermission: Map<RESTPermission, Boolean>)