using UnityEngine;
using Microsoft.MixedReality.WebRTC;
using System.Threading.Tasks;
using System.Collections;

public abstract class RTCDataChannel<T> : MonoBehaviour
{
    private DataChannel channel;
    private Task<DataChannel> makeChannelTask;
    private bool waitingForChannel;
    private bool hasChannel;
    private bool channelOpen;

    public Microsoft.MixedReality.WebRTC.Unity.PeerConnection peerConnectionUnity;
    public bool isSubscriber = false;
    public bool isInitiator;
    public string channelName;
    public T targetData;
    private int x = 0;

    // Start is called before the first frame update
    void Start()
    {
        waitingForChannel = false;
        hasChannel = false;
    }

    // Update is called once per frame
    void Update()
    {
        if (waitingForChannel)
        {
            if (makeChannelTask.IsCompleted)
            {
                channel = makeChannelTask.Result;
                channel.StateChanged += UpdateChannelState;
                channel.MessageReceived += OnMessageReceived;
                hasChannel = true;
                waitingForChannel = false;
            }
            else if (makeChannelTask.IsCanceled || makeChannelTask.IsFaulted)
            {
                Debug.LogError("Failed to start data channel: " + channelName);
            }
        }

        if (!isSubscriber && hasChannel && channelOpen)
        {
            if (x % 1 == 0)
            {
                SendMessage(targetData);
                x = 1;
            }
            else x++;
        }
    }

    public void InitChannel()
    {
        //peerConnectionUnity = GameObject.Find("PeerConnector").GetComponent<Microsoft.MixedReality.WebRTC.Unity.PeerConnection>();
        peerConnectionUnity.Peer.DataChannelAdded += OnDataChannelAdded;
        peerConnectionUnity.Peer.DataChannelRemoved += OnDataChannelRemoved;

        if (isInitiator)
        {
            // Messages should be ordered (i.e. message that is sent first is received first, regardless of if that causes delays)
            // Messages are not reliable, meaning they are not resent or anything if they are dropped (UDP vs TCP?)
            makeChannelTask = peerConnectionUnity.Peer.AddDataChannelAsync(channelName, true, false);
            waitingForChannel = true;
        }
    }

    public void sendData(byte[] msg)
    {
        if (hasChannel && channelOpen)
        {
            channel.SendMessage(msg);
        }
    }

    private void OnDataChannelAdded(DataChannel addedChannel)
    {
        if (!isInitiator && addedChannel.Label.Equals(channelName))
        {
            channel = addedChannel;
            channel.StateChanged += UpdateChannelState;
            channel.MessageReceived += OnMessageReceived;

            Debug.Log("Received Data Channel: " + channelName + " From Peer");

            hasChannel = true;
        }
    }

    private void OnDataChannelRemoved(DataChannel removedChannel)
    {
        if (hasChannel && removedChannel.Label.Equals(channelName))
        {
            hasChannel = false;
            channel = null;
        }
    }

    private void OnMessageReceived(byte[] msg)
    {
        HandleMessage(msg);
    }

    private void UpdateChannelState()
    {
        channelOpen = channel.State == DataChannel.ChannelState.Open;
    }

    protected abstract void HandleMessage(byte[] msg);
    public abstract void SendMessage(T msg);
}
