using UnityEngine;
using Microsoft.MixedReality.WebRTC;
using System.Threading.Tasks;

public class PhoneChannel : MonoBehaviour
{

    private DataChannel channel;
    private Task<DataChannel> makeChannelTask;
    private bool waitingForChannel;
    private bool hasChannel;
    private bool channelOpen;
    private bool isMessageReceived;
    public Microsoft.MixedReality.WebRTC.Unity.PeerConnection peerConnectionUnity;
    public bool isSubscriber = false;
    private bool isInitiator = false;
    public string channelName;
    private int count = 1;

    public ClickCapture cc;
    public GameObject startInd;
    public GameObject endInd;

    void Start()
    {
        cc.callback += OnGestureCallback;

        waitingForChannel = false;
        hasChannel = false;
        channelOpen = false;
    }

    // Update is called once per frame
    void Update()
    {
        // Wait for channel to initialize
        if (waitingForChannel)
        {
            if (makeChannelTask.IsCompleted)
            {
                channel = makeChannelTask.Result;
                channel.StateChanged += UpdateChannelState;
                channel.MessageReceived += OnMessageReceived;
                hasChannel = true;
                Debug.Log("Data Channel " + channelName + " Created");
                waitingForChannel = false;
            }
            else if (makeChannelTask.IsCanceled || makeChannelTask.IsFaulted)
            {
                Debug.LogError("Failed to start data channel: " + channelName);
            }
        }


        //React to incoming messages
        if (isMessageReceived)
        {
            isMessageReceived = false;
        }
    }

    public void OnGestureCallback(Vector2 startPos, Vector2 endPos, float duration, bool willContinue)
    {
        Debug.Log("Start: " + startPos.x.ToString("N") + ", " + startPos.y.ToString("N"));
        Debug.Log("End: " + endPos.x.ToString("N") + ", " + endPos.y.ToString("N"));
        Debug.Log("Duration: " + duration.ToString("N"));

        Vector3 p = startInd.transform.position;
        p.x = startPos.x * transform.localScale.x + transform.position.x - transform.localScale.x / 2;
        p.y = startPos.y * transform.localScale.y + transform.position.y - transform.localScale.y / 2;
        startInd.transform.position = p;

        Vector3 v = endInd.transform.position;
        v.x = endPos.x * transform.localScale.x + transform.position.x - transform.localScale.x / 2;
        v.y = endPos.y * transform.localScale.y + transform.position.y - transform.localScale.y / 2;
        endInd.transform.position = v;

        SendMessage(startPos, endPos, duration, willContinue);
    }

    public void InitChannel(bool init)
    {
        isInitiator = init;
        peerConnectionUnity.Peer.DataChannelAdded += OnDataChannelAdded;
        peerConnectionUnity.Peer.DataChannelRemoved += OnDataChannelRemoved;

        if (isInitiator)
        {
            // Messages should be ordered (i.e. message that is sent first is received first, regardless of if that causes delays)
            // Messages are not reliable, meaning they are not resent or anything if they are dropped (UDP vs TCP?)
            makeChannelTask = peerConnectionUnity.Peer.AddDataChannelAsync(channelName, true, false);
            waitingForChannel = true;
            Debug.Log("Creating Data Channel: " + channelName);
        }
    }

    private byte[] Float2Bytes(float val)
    {
        int num = (int)(val * 1000);
        byte[] b = new byte[2];
        b[0] = (byte)((num & 0xFF00) >> 8);
        b[1] = (byte)(num & 0x00FF);
        return b;
    }

    public void SendMessage(Vector2 start, Vector2 end, float duration, bool willContinue)
    {
        byte[] bmsg = new byte[11];

        byte[] sx = Float2Bytes(start.x);
        byte[] sy = Float2Bytes(start.y);
        byte[] ex = Float2Bytes(end.x);
        byte[] ey = Float2Bytes(end.y);
        byte[] d = Float2Bytes(duration);

        for (int i = 0; i < 2; i++)
        {
            bmsg[i] = sx[i];
            bmsg[i + 2] = sy[i];
            bmsg[i + 4] = ex[i];
            bmsg[i + 6] = ey[i];
            bmsg[i + 8] = d[i];
        }

        if (willContinue) bmsg[10] = 0xff;
        else bmsg[10] = 0x00;

        if (hasChannel && channelOpen)
        {
            channel.SendMessage(bmsg);
        }
    }

    #region Callbacks
    private void OnDataChannelAdded(DataChannel addedChannel)
    {
        if (!isInitiator && addedChannel.Label.Equals(channelName))
        {
            channel = addedChannel;
            channel.StateChanged += UpdateChannelState;
            channel.MessageReceived += OnMessageReceived;
            hasChannel = true;
            Debug.Log("Channel: " + channelName + " Received from Peer");
        }
    }

    private void OnDataChannelRemoved(DataChannel removedChannel)
    {
        if (hasChannel && removedChannel.Label.Equals(channelName))
        {
            hasChannel = false;
            channel = null;
            Debug.Log(channelName + " Channel Removed");
        }
    }

    private void OnMessageReceived(byte[] msg)
    {
        isMessageReceived = true;
    }

    private void UpdateChannelState()
    {
        channelOpen = channel.State == DataChannel.ChannelState.Open;
    }
    #endregion

    #region Helper Methods

    private byte[] VectorToBytes(Vector3 v, int count = -1)
    {
        byte[] b;
        if (count < 0)
            b = new byte[12];
        else
        {
            b = new byte[13];
            b[12] = (byte)count;
        }

        int next = 0;
        for (int i = 0; i < 3; i++)
        {
            byte[] vecBytes = System.BitConverter.GetBytes(v[i]);
            for (int j = 0; j < vecBytes.Length; j++)
            {
                b[next] = vecBytes[j];
                next++;
            }
        }

        return b;
    }
    #endregion
}
