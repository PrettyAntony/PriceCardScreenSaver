/*
*   IntentService to download video file
 *   Downloads will be queued
 *     Download file from remote url and saved at local path (Application's external files directory ) by the name "guid" returned by web server
 *     Check the file already exists.
 *     A temporary file is created with the filename filename+ "temp"  which is renamed to actual file name on successful completion.
 *     Before download if a temporary file exists , download resume.
 *     After download complete send a broadcast that file has been downloaded
 */
public class VideoDownloadService extends  JobIntentService {

public static final int NOTIFICATION_ID=101;
    public VideoDownloadService() {
       //super(TAG);
    }

    /* */
    public static void startVideoDownloadAction(Context context, String url, int type) {
        Intent intent = new Intent(context, VideoDownloadService.class);
        intent.putExtra(INTENT_PARAM_URL, url);
        intent.putExtra(INTENT_PARAM_VIDEO_TYPE, type);
       // context.startService(intent);
        enqueueWork(context,VideoDownloadService.class,101,intent);
    }


    @Override
    protected void onHandleWork(Intent intent) {
        if (intent != null) {
            int videoType = intent.getIntExtra(INTENT_PARAM_VIDEO_TYPE,0);
            String url = intent.getStringExtra(INTENT_PARAM_URL);
            final String fileName = url.substring(url.lastIndexOf("/") + 1);
            try {
              boolean success=  downloadFile(url, FileDownloadUtils.getVideoDownloadPath(this.getApplicationContext()), fileName);
              if(success){
                  sendBroadcast(videoType,fileName,true);
              }
            } catch (Exception e) {
                LogUtil.logException(TAG, e);
                sendBroadcast(videoType,fileName,false);
            }

            //Download file from the url
        }
    }


    /**
     * @param remoteFilePath
     * @param folderDir      Should be dir/
     * @param fileName
     * @throws Exception
     */
    public   synchronized boolean downloadFile(String remoteFilePath,
                                                    String folderDir,
                                                    String fileName
    ) throws Exception {
        InputStream input = null;
        OutputStream outStream = null;

        String localTempFileName;
        File targetFile;
        try {

            LogUtil.logString("downloading video file url=" + remoteFilePath);
           //create  folder if it does not exists...
            File folder= new File(folderDir) ;
            folder.mkdirs();

            targetFile = new File(folderDir, fileName);

            localTempFileName = "temp_" + fileName;

            if (targetFile.exists()) {
                targetFile.setLastModified(System.currentTimeMillis());
                LogUtil.logErrorString("Video file download: file exists: Not downloading again" + targetFile.getAbsolutePath());
                return false;
            } else {
                showNotification();
                URL url = new URL(remoteFilePath);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                File targetTempFile = new File(folderDir, localTempFileName);
                connection.setDoInput(true);
                connection.setConnectTimeout(60000);
                connection.setReadTimeout(60000);


                int downloaded = 0;
                if (targetTempFile.exists()) {
                    downloaded = (int) targetTempFile.length();
                    connection.setRequestProperty("Range", "bytes=" + downloaded + "-");
                } else {
                    connection.setRequestProperty("Range", "bytes=" + downloaded + "-");
                }
                connection.connect();
                input = connection.getInputStream();
                outStream=  new FileOutputStream(targetTempFile.getAbsolutePath(),true);
                byte[] buffer = new byte[8 * 1024];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                    LogUtil.logString("Video file writing: 8kb");
                }

                targetTempFile.renameTo(targetFile);
                targetFile.setLastModified(System.currentTimeMillis());
                LogUtil.logString("Video file download complete: " + targetFile.getAbsolutePath());
                cancelNotification();
                return true;
            }
            //return fileName if success

        } catch (IOException e) {
            LogUtil.logException(TAG, e);
            throw e;

        } catch (Exception e) {
            LogUtil.logException(TAG, e);
        } finally {
            try {
                if (outStream != null)
                    outStream.close();
            } catch (Exception e) {
                LogUtil.logException(TAG, e);
            }
            try {
                if (input != null)
                    input.close();
            } catch (Exception e) {
                LogUtil.logException(TAG, e);
            }


        }

return false;

    }

    public void sendBroadcast(int videoType,String fileName,boolean success) {


    Intent localIntent = new Intent();
        localIntent
                .setAction(PromotionFragment.BROADCAST_ACTION_VIDEO_FILE_DOWNLOADED);
        localIntent.addCategory(Intent.CATEGORY_DEFAULT);
        localIntent.putExtra(INTENT_PARAM_VIDEO_TYPE,videoType);
        localIntent.putExtra(INTENT_PARAM_FILE_NAME,fileName);
        localIntent.putExtra(INTENT_PARAM_DOWNLOAD_SUCCESS,success);
        try

    {
        LocalBroadcastManager broadcastMgr = LocalBroadcastManager
                .getInstance(getApplicationContext());
        broadcastMgr.sendBroadcast(localIntent);
    } catch(
    Exception e)

    {
        LogUtil.logException(TAG, e);
    }
}
    public void showNotification() {
    NotificationCompat.Builder notification = new NotificationCompat.Builder(PriceCardApp.getmPricecardApp(), "video_download");
    //Title for Notification
    notification.setContentTitle("Downloading promotion videos");
    //Message in the Notification
    //notification.setContentText(contentText);
    //Alert shown when Notification is received
        notification.setSmallIcon( R.drawable.ic_launcher);
    notification.setTicker("Downloading promotion videos");

            NotificationManager manager = (NotificationManager) PriceCardApp.getmPricecardApp().getSystemService(Context.NOTIFICATION_SERVICE);
    manager.notify(NOTIFICATION_ID, notification.build());
    }
    public void cancelNotification() {
        final Context appContext =PriceCardApp.getmPricecardApp();
        NotificationManager notificationManager= (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }
}