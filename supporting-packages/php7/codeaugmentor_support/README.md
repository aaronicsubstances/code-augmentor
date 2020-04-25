## Install Notes on Windows 10
composer install
took so long
Setting SSL endpoint with
composer config --global repo.packagist composer https://packagist.org
fixed it up

composer dump-autoload 
needed when using classmap instead of PSR-4

composer require --dev phpunit/phpunit "^7"
to install dependency not meant for library consumers

So to generate optimised autoload files, just need to run:
composer dump-autoload -o