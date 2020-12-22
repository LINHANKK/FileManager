# FileManager
文件管理器



当接入到主板，或者其他设备时，需要签名文件
具体：
1.先在Mainfest文件中加入 android:sharedUserId="android.uid.system" ，加入到<manifest>标签中
  
2.BUILD--build bundle/APK--BUILD APK--locale

3.选择app-debug.apk，将其复制到sign文件夹中

4.更改sign -launcher.bat文件中的内容为：java -jar signapk.jar platform.x509.pem platform.pk8 app-debug.apk .\app-debugsign.apk

5.在sign文件夹中找到app-debugsign.apk，并将其在终端运行，使用adb install “该文件的路径” （需先连接adb），详见adb-shell文档
