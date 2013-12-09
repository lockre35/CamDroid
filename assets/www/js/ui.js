(function () {
	//Documentation for this plugin can be found here https://wiki.videolan.org/Documentation:WebPlugin/
    var host = window.location.hostname;
    var port = window.location.port;
    var videoPlugin;
    var videoStream;
    var audioPlugin;
    var audioStream;
    var error;
    var volume;

    function stream(object,type,done) {

	     var state = "idle";
        
	     return {

	         restart: function () {
                state = "restarting";
                done();
		          if (object.playlist.isPlaying) {
		              object.playlist.stop();
		              object.playlist.clear();
		              object.playlist.items.clear();
                    setTimeout(function () {
                        this.start(false);
                    }.bind(this),2000);
		          } else {
                    this.start(false);
                }
	         },

	         start: function (e) {
                var req = generateUriParams(type);
                state = "starting";
                if (e!==false) done();
                $.ajax({
                    type: 'GET', 
                    url: 'spydroid.sdp?id='+(type==='video'?0:1)+'&'+req.uri, 
                    success: function (e) {
                        setTimeout(function () {
                            state = "streaming";
                            object.playlist.add('http://'+host+':'+port+'/spydroid.sdp?id='+(type==='video'?0:1)+'&'+req.uri,'',req.params);
		                      object.playlist.playItem(0);
                            setTimeout(function () {
                                done();
                            },600);
                        },1000);
                    }, 
                    error: function () {
                        state = "error";
                        getError();
                    }
                });
		      },

	         stop: function () {
                $.ajax({
                    type: 'GET', 
                    url: 'spydroid.sdp?id='+(type==='video'?0:1)+'&stop', 
                    success: function (e) {
                        //done(); ??
                    }, 
                    error: function () {
                        state = "error";
                        getError();
                    }
                });
		          if (object.playlist.isPlaying) {
		              object.playlist.stop();
		              object.playlist.clear();
		              object.playlist.items.clear();               
		          }
                state = "idle";
                done();
	         },

	         getState: function() {
                return state;
	         },

	         isStreaming: function () {
		          return object.playlist.isPlaying;
	         }

	     }
	     
    }



   
    function generateUriParams(type) {
	     var audioEncoder, videoEncoder, cache, rotation, flash, camera, res;

	     // Audio conf
	         audioEncoder = 'aac';

	     
	     // Resolution

	     res = /([0-9]+)x([0-9]+)/.exec('640x480');

	     // Video conf


	         videoEncoder = ('h264')+'='+
		          /[0-9]+/.exec('1000 kbps')[0]+'-'+
		          /[0-9]+/.exec('20 fps')[0]+'-';
	         videoEncoder += res[1]+'-'+res[2];
	   
 
	     // Flash
	     flash = 'off';

	     // Camera
	     camera = null;

	     // Params
	     cache = /[0-9]+/.exec('1000 ms')[0];	    

	     return {
	         uri:type==='audio'?audioEncoder:(videoEncoder+'&flash='+flash+'&camera='+camera),
	         params:[':network-caching='+cache]
	     }
    }

    function getError() {
        sendRequest(
            'state',
            function (e) {
                error = e.state.lastError;
                updateStatus();
            },
            function () {
                error = 'Phone unreachable !';
                updateStatus();
            }         
        );
    }

     function updateStatus() {
	     var status = $('#status'), button = $('#connect>div>h1'), cover = $('#vlc-container #upper-layer');

	     // STATUS
	     if (videoStream.getState()==='starting' || videoStream.getState()==='restarting' || 
	         audioStream.getState()==='starting' || audioStream.getState()==='restarting') {
	         status.text(('Trying to connect...'))
	     } else {
	         if (!videoStream.isStreaming() && !audioStream.isStreaming()) status.text(('NOT CONNECTED')); 
	         else if (videoStream.isStreaming() && !audioStream.isStreaming()) status.html(('Streaming video but not audio'));
	         else if (!videoStream.isStreaming() && audioStream.isStreaming()) status.html(('Streaming audio but not video'));
	         else status.text(('Streaming audio and video'));
	     }


	     // WINDOW
	     if (videoStream.getState()==='streaming') {
	         videoPlugin.css('visibility','inherit');
	         cover.hide();
	     }

	     if (videoStream.getState()==='idle') {
	         if (audioStream.getState()==='streaming') {
		          videoPlugin.css('visibility','hidden'); 
		          cover.html('').css('background','url("images/out2.png") center no-repeat').show();
	         } else if (audioStream.getState()==='idle') {
		          videoPlugin.css('visibility','hidden'); 
		          cover.html('').css('background','url("images/out2.png") center no-repeat').show();
	         }
	     }

    }

    function disableAndEnable(input) {
	     input.prop('disabled',true);
	     setTimeout(function () {
	         input.prop('disabled',false);
	     },1000);
    }

    function setupEvents() {

	     var cover = $('#vlc-container #upper-layer');
	     var status = $('#status');
	     var button = $('#connect>div>h1');	

	     $('.popup #close').click(function (){
	         $('#glass').fadeOut();
	         $('.popup').fadeOut();
	     });
	    
	     $('#begin').click(function () {
	         if((videoStream.getState()=='idle'||audioStream.getState()=='idle')){
		          videoStream.start();
		          audioStream.start();
	         }
	     });
	     
	     $('#stop').click(function (){
	    	 if ((videoStream.getState()!=='idle' && videoStream.getState()!=='error') || 
		          (audioStream.getState()!=='idle' && audioStream.getState()!=='error')) {
		          videoStream.stop();
		          audioStream.stop();
	         }
	     });


	     $('#fullscreen').click(function () {
	         videoPlugin[0].video.toggleFullscreen();
	     });
	     
	     $('#mute').click(function(){
	       	audioPlugin[0].audio.toggleMute();
	      });
	      



        window.onbeforeunload = function (e) {
            videoStream.stop();
            audioStream.stop();
        }

	 };

    
    $(document).ready(function () {
     
        videoPlugin = $('#vlcv');
        videoStream = stream(videoPlugin[0],'video',updateStatus);
        audioPlugin = $('#vlca');
        audioStream = stream(audioPlugin[0],'audio',updateStatus);

        

	     $('.popup').each(function () {
	         $(this).css({'top':($(window).height()-$(this).height())/2,'left':($(window).width()-$(this).width())/2});
	     });

	     $('#tooltip').hide();
	     $('#need-help').show();

	     // Bind DOM events
	     setupEvents();
	     
	     videoStream.start();
         audioStream.start();

    });


}());
