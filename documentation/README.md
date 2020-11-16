Overview
=====
This project contains 2 parts. First one is about multi login mechanisms from the Living app. Second one is about Lucene search query on top of BDM using Spring Data JPA specifications.

Multi authentications
=====
The application is composed of 3 parts :
An external login (classic and Google)
An internal login (Facebook)
A private app only for non guest users

To allow guest users, you need to allow it in 
## authenticationManager-config.properties
``` properties
auth.tenant.guest.active=true
auth.tenant.guest.username=guest
auth.tenant.guest.password=guestPwd
auth.tenant.guest.apps=[publicAuth,internalAuth]
```

You need to give him the same right as configured in the REST API Extension :
## custom-permissions-mapping.properties
``` properties
user|guest=[guestAuth]
```

Note : Starting the studio redeploy the organization. It will remove the profile from users created through the Rest API extensions

Lucene search over BDM
=====

This part allow you to create dynamic filters to search BDM objects without writing SQL. It's based on Lucene Query syntax and JPA Specifications.
You can write queries with AND, OR operands, wildcards, ranges, etc.
```
persistenceId:1 AND active:true AND attribute.name:"Bar" AND attribute.type.name:"Foo" 
persistenceId:[1 TO 10]  OR  attribute.name:"Bonita*"  
```

The code is in **dataSearchRestAPI** and to call it, you also need to pass the entity you're looking for :

../API/extension/search?**entity=MyEntity**&q=attribut.subAttribute:"Bonita*" AND attribut2:3
