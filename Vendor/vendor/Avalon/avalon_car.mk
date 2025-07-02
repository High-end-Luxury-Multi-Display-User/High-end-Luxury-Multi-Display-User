PRODUCT_PACKAGES += MultiDisplayRpiOverlays AndroidRpiFrameWorkMultiDisplay libgpiod libgpiohalrpi5 libads1115 Rpi5GPIOTest DriverLauncher MultiDisplaySecondaryHomeTestLauncher

#         vendor/Avalon/init.driverlauncher.rc:root/init.driverlauncher.rc \
# MultiDisplaySecondaryHomeTestLauncher DriverLauncher

PRODUCT_COPY_FILES += \
	vendor/Avalon/init.avalonhw.rc:root/init.avalonhw.rc \
	vendor/Avalon/MUMD/InputPortAssociation/input-port-associations.xml:$(TARGET_COPY_OUT_VENDOR)/etc/input-port-associations.xml

