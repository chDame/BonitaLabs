angular.module("iframeOnload", []).directive("iframeSetDimensionsOnload", [function(){
return {
    restrict: 'A',
    link: function(scope, element, attrs){
        element.on('load', function(){
               //element.css('width', '100%');
            /* Set the dimensions here, 
               I think that you were trying to do something like this: */
              /* var body =  element[0].contentWindow.document.body;
               var body2 =  element[0].contentDocument.body;
               var html =  element[0].contentWindow.document.documentElement;
               var height = Math.max( body2.scrollHeight, body2.offsetHeight, body.scrollHeight, body.offsetHeight, body.clientHeight,
        html.clientHeight, html.scrollHeight, html.offsetHeight, body2.clientHeight );
               var iFrameHeight = height + 'px';
               element.css('height', iFrameHeight);*/
               var cssLink = document.createElement("link");
                cssLink.href = top.location.origin + top.location.pathname+"../theme/theme.css"; 
                cssLink.rel = "stylesheet"; 
                cssLink.type = "text/css"; 
               element[0].contentDocument.head.appendChild(cssLink);
        })
    }
}}]);