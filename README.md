# ofbiz-plugs


## Install plugin
* Extract the plugin if it is compressed
* Place the extracted directory into /plugins
* Run `gradlew installPlugin -PpluginId=myplugin`

## Uninstall plugin
* Run `gradlew uninstallPlugin -PpluginId=myplugin`

## Remove plugin
* Run `gradlew removePlugin -PpluginId=myplugin`

## Create a new plugin
* Run `gradlew createPlugin -PpluginId=myplugin`
