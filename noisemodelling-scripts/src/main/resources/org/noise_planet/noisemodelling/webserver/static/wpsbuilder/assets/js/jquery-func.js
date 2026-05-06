!function ($) {
	//=================================== scroll  ===================================//

$body.scrollspy({
      target: '#navbar-main',
      offset: navHeight
    })

    $window.on('load', function () {
      $body.scrollspy('refresh')
    })

    $('#navbar-main [href=#]').click(function (e) {
      e.preventDefault()
    })


});
